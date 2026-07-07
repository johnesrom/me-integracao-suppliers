package com.exemplo.mesuppliers.features.authentication.service;

import com.exemplo.mesuppliers.config.MeApiProperties;
import com.exemplo.mesuppliers.exception.MeApiClientException;
import com.exemplo.mesuppliers.features.authentication.dto.AuthTokenRequestDto;
import com.exemplo.mesuppliers.features.authentication.dto.AuthTokenResponseDto;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationServiceImpl.class);
    private static final long DEFAULT_TOKEN_TTL_SECONDS = 300L;

    private final WebClient meWebClient;
    private final MeApiProperties meApiProperties;
    private volatile CachedToken cachedToken;

    public AuthenticationServiceImpl(WebClient meWebClient, MeApiProperties meApiProperties) {
        this.meWebClient = meWebClient;
        this.meApiProperties = meApiProperties;
    }

    @Override
    public String getValidBearerToken() {
        CachedToken snapshot = this.cachedToken;
        if (snapshot != null && Instant.now().isBefore(snapshot.expiresAt())) {
            return snapshot.bearerToken();
        }

        synchronized (this) {
            snapshot = this.cachedToken;
            if (snapshot != null && Instant.now().isBefore(snapshot.expiresAt())) {
                return snapshot.bearerToken();
            }
            return requestAndCacheToken();
        }
    }

    @Override
    public synchronized String refreshBearerToken() {
        return requestAndCacheToken();
    }

    private String requestAndCacheToken() {
        AuthTokenRequestDto request = new AuthTokenRequestDto(meApiProperties.getClientId(), meApiProperties.getClientSecret());
        try {
            AuthTokenResponseDto response = meWebClient.post()
                    .uri("/v1/auth/tokens")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AuthTokenResponseDto.class)
                    .block();

            if (response == null || !StringUtils.hasText(response.getAccessToken())) {
                throw new IllegalStateException("Resposta de autenticacao invalida: token nao retornado");
            }

            long expiresIn = response.getExpiresIn() == null ? DEFAULT_TOKEN_TTL_SECONDS : response.getExpiresIn();
            long adjusted = Math.max(expiresIn - meApiProperties.getTokenSkewSeconds(), 1L);
            Instant expiresAt = Instant.now().plusSeconds(adjusted);
            String bearerToken = normalizeBearerToken(response.getTokenType(), response.getAccessToken());
            this.cachedToken = new CachedToken(bearerToken, expiresAt);

            log.info("Token do ME atualizado com sucesso. Expira em {} segundos.", adjusted);
            return bearerToken;
        } catch (WebClientResponseException ex) {
            log.error("Erro HTTP ao autenticar no ME: status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new MeApiClientException(ex.getStatusCode().value(), ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("Erro inesperado ao autenticar no ME", ex);
            throw new IllegalStateException("Falha ao autenticar na API do Mercado Eletronico", ex);
        }
    }

    private String normalizeBearerToken(String tokenType, String accessToken) {
        if (accessToken.startsWith("Bearer ")) {
            return accessToken;
        }
        if (StringUtils.hasText(tokenType) && !"Bearer".equalsIgnoreCase(tokenType)) {
            return tokenType + " " + accessToken;
        }
        return "Bearer " + accessToken;
    }

    private record CachedToken(String bearerToken, Instant expiresAt) {}
}
