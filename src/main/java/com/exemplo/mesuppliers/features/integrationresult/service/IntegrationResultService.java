package com.exemplo.mesuppliers.features.integrationresult.service;

import com.exemplo.mesuppliers.features.integrationresult.dto.IntegrationResultWebhookDto;

public interface IntegrationResultService {
    void process(IntegrationResultWebhookDto webhookDto);
}
