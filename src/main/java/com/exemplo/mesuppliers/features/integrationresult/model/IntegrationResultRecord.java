package com.exemplo.mesuppliers.features.integrationresult.model;

import java.time.Instant;

public record IntegrationResultRecord(
        String correlationId,
        String identifier,
        Integer statusCode,
        String status,
        String message,
        Instant sentAt,
        Instant webhookReceivedAt
) {
}
