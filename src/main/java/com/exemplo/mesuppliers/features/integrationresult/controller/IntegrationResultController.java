package com.exemplo.mesuppliers.features.integrationresult.controller;

import com.exemplo.mesuppliers.features.integrationresult.dto.IntegrationResultWebhookDto;
import com.exemplo.mesuppliers.features.integrationresult.service.IntegrationResultService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IntegrationResultController {

    private final IntegrationResultService integrationResultService;

    public IntegrationResultController(IntegrationResultService integrationResultService) {
        this.integrationResultService = integrationResultService;
    }

    @PostMapping("/integration/result")
    public ResponseEntity<Void> receiveIntegrationResult(@Valid @RequestBody IntegrationResultWebhookDto webhookDto) {
        integrationResultService.process(webhookDto);
        return ResponseEntity.accepted().build();
    }
}
