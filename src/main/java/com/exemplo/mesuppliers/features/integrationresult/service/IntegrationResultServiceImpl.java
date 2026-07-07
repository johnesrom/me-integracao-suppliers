package com.exemplo.mesuppliers.features.integrationresult.service;

import com.exemplo.mesuppliers.exception.InvalidWebhookPayloadException;
import com.exemplo.mesuppliers.features.integrationresult.dto.IntegrationResultWebhookDto;
import com.exemplo.mesuppliers.features.integrationresult.model.IntegrationResultRecord;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IntegrationResultServiceImpl implements IntegrationResultService {

    private static final Logger log = LoggerFactory.getLogger(IntegrationResultServiceImpl.class);
    private final IntegrationResultPersistenceService integrationResultPersistenceService;

    public IntegrationResultServiceImpl(IntegrationResultPersistenceService integrationResultPersistenceService) {
        this.integrationResultPersistenceService = integrationResultPersistenceService;
    }

    @Override
    public void process(IntegrationResultWebhookDto webhookDto) {
        if (!"integration.result".equalsIgnoreCase(webhookDto.getTopic())) {
            throw new InvalidWebhookPayloadException("Topico invalido para esta rota: " + webhookDto.getTopic());
        }

        IntegrationResultWebhookDto.DataPayload data = webhookDto.getData();
        log.info("Webhook integration.result recebido. topic={} resource={} correlationId={} identifier={} statusCode={} status={}",
                webhookDto.getTopic(), data.getResource(), data.getCorrelationId(), data.getIdentifier(), data.getStatusCode(), data.getStatus());

        IntegrationResultRecord record = new IntegrationResultRecord(
                data.getCorrelationId(),
                data.getIdentifier(),
                data.getStatusCode(),
                data.getStatus(),
                data.getMessage(),
                null,
                Instant.now()
        );

        integrationResultPersistenceService.persist(data.getResource(), record);
    }
}
