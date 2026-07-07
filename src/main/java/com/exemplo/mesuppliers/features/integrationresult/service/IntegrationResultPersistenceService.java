package com.exemplo.mesuppliers.features.integrationresult.service;

import com.exemplo.mesuppliers.features.integrationresult.model.IntegrationResultRecord;

public interface IntegrationResultPersistenceService {
    void persist(String resource, IntegrationResultRecord record);
    boolean supports(String resource);
}
