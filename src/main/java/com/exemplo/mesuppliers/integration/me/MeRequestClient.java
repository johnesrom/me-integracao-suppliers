package com.exemplo.mesuppliers.integration.me;

import com.exemplo.mesuppliers.exception.MeApiClientException;
import com.exemplo.mesuppliers.features.authentication.service.AuthenticationService;
import com.exemplo.mesuppliers.features.integrationresult.model.IntegrationResultRecord;
import com.exemplo.mesuppliers.features.integrationresult.service.IntegrationResultPersistenceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class MeRequestClient {

    private static final Logger log = LoggerFactory.getLogger(MeRequestClient.class);
    private static final String X_ME_CORRELATION_ID = "X-ME-CORRELATION-ID";
    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    private final AuthenticationService authenticationService;
    private final WebClient meWebClient;
    private final ObjectMapper objectMapper;
    private final IntegrationResultPersistenceService integrationResultPersistenceService;
    private final int maxAttempts;
    private final long initialBackoffMs;

    public MeRequestClient(
            AuthenticationService authenticationService,
            WebClient meWebClient,
            ObjectMapper objectMapper,
            IntegrationResultPersistenceService integrationResultPersistenceService,
            @Value("${me.api.retry.max-attempts:4}") int maxAttempts,
            @Value("${me.api.retry.initial-backoff-ms:1000}") long initialBackoffMs
    ) {
        this.authenticationService = authenticationService;
        this.meWebClient = meWebClient;
        this.objectMapper = objectMapper;
        this.integrationResultPersistenceService = integrationResultPersistenceService;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.initialBackoffMs = Math.max(100L, initialBackoffMs);
    }

    public String send(String module, HttpMethod method, String uri, Object payload, String meCorrelationId, Object... uriVariables) {
        String bearerToken = authenticationService.getValidBearerToken();
        persistSendTimestamp(module, meCorrelationId);
        log.info("Enviando requisicao para ME. correlationIdHeader={}", meCorrelationId);

        try {
            String responseBody = sendWithRetry(bearerToken, method, uri, payload, meCorrelationId, uriVariables);
            String correlationId = extractCorrelationId(responseBody);
            log.info("Resposta recebida do ME. correlationId={}", correlationId);
            return responseBody;
        } catch (WebClientResponseException ex) {
            log.error("Erro HTTP ao chamar o ME. module={} body={}", module, ex.getResponseBodyAsString());
            persistErrorResult(module, meCorrelationId, ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new MeApiClientException(ex.getStatusCode().value(), ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("Erro inesperado ao chamar o ME. module={}, ex={}", module, ex);
            persistErrorResult(module, meCorrelationId, 500, ex.getMessage());
            throw new IllegalStateException("Falha de comunicacao com o Mercado Eletronico", ex);
        }
    }

    private String sendWithRetry(String bearerToken, HttpMethod method, String uri, Object payload, String meCorrelationId, Object... uriVariables) {
        long backoffMs = initialBackoffMs;
        WebClientResponseException lastRateLimitException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                WebClient.RequestBodySpec request = meWebClient.method(method)
                        .uri(uri, uriVariables)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken);

                if (StringUtils.hasText(meCorrelationId)) {
                    request.header(X_ME_CORRELATION_ID, meCorrelationId);
                }

                RequestHeadersSpec<?> requestSpec = payload == null ? request : request.bodyValue(payload);
                return requestSpec.retrieve().bodyToMono(String.class).block();
            } catch (WebClientResponseException ex) {
                HttpStatusCode statusCode = ex.getStatusCode();
                if (statusCode.value() != HTTP_TOO_MANY_REQUESTS || attempt == maxAttempts) {
                    throw ex;
                }

                lastRateLimitException = ex;
                long waitMs = getRetryAfterMs(ex).orElse(backoffMs);
                log.warn("Rate limit no ME (429). attempt={}/{} aguardando {}ms para retry.", attempt, maxAttempts, waitMs);
                sleep(waitMs);
                backoffMs = Math.min(backoffMs * 2, 15000L);
            }
        }

        throw lastRateLimitException == null
                ? new IllegalStateException("Falha ao chamar o ME")
                : lastRateLimitException;
    }

    private Optional<Long> getRetryAfterMs(WebClientResponseException ex) {
        String retryAfter = ex.getHeaders().getFirst(HttpHeaders.RETRY_AFTER);
        if (!StringUtils.hasText(retryAfter)) {
            return Optional.empty();
        }

        try {
            long seconds = Long.parseLong(retryAfter.trim());
            if (seconds <= 0) return Optional.empty();
            return Optional.of(seconds * 1000L);
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Execucao interrompida durante backoff de retry", ex);
        }
    }

    private String extractCorrelationId(String responseBody) {
        if (!StringUtils.hasText(responseBody)) return null;
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode node = root.get("correlationId");
            return node != null && !node.isNull() ? node.asText() : null;
        } catch (Exception ex) {
            log.warn("Nao foi possivel extrair correlationId da resposta do ME. body={}", responseBody);
            return null;
        }
    }

    private void persistErrorResult(String module, String correlationId, Integer statusCode, String message) {
        if (!StringUtils.hasText(correlationId)) return;
        String resource = mapModuleToResource(module);
        if (!integrationResultPersistenceService.supports(resource)) return;

        IntegrationResultRecord record = new IntegrationResultRecord(correlationId, null, statusCode, "ERROR", message, null, null);
        try {
            integrationResultPersistenceService.persist(resource, record);
        } catch (Exception ex) {
            log.error("Falha ao persistir erro do ME em tabela temporaria. resource={} correlationId={}", resource, correlationId, ex);
        }
    }

    private void persistSendTimestamp(String module, String correlationId) {
        if (!StringUtils.hasText(correlationId)) return;
        String resource = mapModuleToResource(module);
        if (!integrationResultPersistenceService.supports(resource)) return;

        IntegrationResultRecord record = new IntegrationResultRecord(correlationId, null, null, "PENDING", null, Instant.now(), null);
        try {
            integrationResultPersistenceService.persist(resource, record);
        } catch (Exception ex) {
            log.error("Falha ao persistir data/hora de envio em tabela temporaria. resource={} correlationId={}", resource, correlationId, ex);
        }
    }

    private String mapModuleToResource(String module) {
        if (!StringUtils.hasText(module)) return "";
        String normalized = module.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "suppliers", "supplier" -> "supplier";
            default -> normalized;
        };
    }
}
