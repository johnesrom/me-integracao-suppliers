package com.exemplo.mesuppliers.schedulers.suppliers;

import com.exemplo.mesuppliers.exception.MeApiClientException;
import com.exemplo.mesuppliers.features.suppliers.dto.SupplierPayloadDto;
import com.exemplo.mesuppliers.features.suppliers.dto.SupplierResponseDto;
import com.exemplo.mesuppliers.features.suppliers.repository.SupplierSolicitacaoRepository;
import com.exemplo.mesuppliers.features.suppliers.service.SupplierService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "me.integration.suppliers", name = "enabled", havingValue = "true")
public class SupplierSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(SupplierSyncScheduler.class);
    private static final String ACTION_UPDATE = "UPDATE";

    private final SupplierSolicitacaoRepository repository;
    private final SupplierService service;
    private final ObjectMapper objectMapper;
    private final long requestIntervalMs;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public SupplierSyncScheduler(
            SupplierSolicitacaoRepository repository,
            SupplierService service,
            ObjectMapper objectMapper,
            @Value("${me.integration.suppliers.request-interval-ms:250}") long requestIntervalMs
    ) {
        this.repository = repository;
        this.service = service;
        this.objectMapper = objectMapper;
        this.requestIntervalMs = Math.max(0L, requestIntervalMs);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() { runSync("startup"); }

    @Scheduled(cron = "${me.integration.suppliers.cron:0 */5 * * * *}")
    public void sync() { runSync("cron"); }

    private void runSync(String trigger) {
        if (!running.compareAndSet(false, true)) return;
        try {
            List<SupplierPayloadDto> payloads = repository.findAllForSync();
            for (SupplierPayloadDto payload : payloads) {
                String action = payload.getSyncAction();
                try {
                    log.info("Payload supplier. trigger={} action={} payload={}", trigger, action, toJson(payload));
                    SupplierResponseDto response = ACTION_UPDATE.equalsIgnoreCase(action) ? service.update(payload) : service.create(payload);
                    log.info("Supplier enviado. trigger={} action={} id={} status={} msg={}", trigger, action, response.getId(), response.getStatus(), response.getMessage());
                } catch (MeApiClientException ex) {
                    log.error("Erro ME supplier. trigger={} action={} status={} body={}", trigger, action, ex.getStatusCode(), ex.getResponseBody());
                }
                sleepBetweenRequests();
            }
        } finally {
            running.set(false);
        }
    }

    private String toJson(SupplierPayloadDto payload) {
        try { return objectMapper.writeValueAsString(payload); }
        catch (JsonProcessingException ex) { return "{\"error\":\"serialize\"}"; }
    }

    private void sleepBetweenRequests() {
        if (requestIntervalMs <= 0) return;
        try {
            Thread.sleep(requestIntervalMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Execucao interrompida durante intervalo entre requisicoes", ex);
        }
    }
}
