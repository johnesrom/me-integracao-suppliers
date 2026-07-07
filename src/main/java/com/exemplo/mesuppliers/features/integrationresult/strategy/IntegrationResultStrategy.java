package com.exemplo.mesuppliers.features.integrationresult.strategy;

import com.exemplo.mesuppliers.features.integrationresult.model.IntegrationResultRecord;

public interface IntegrationResultStrategy {
    String resource();
    void persist(IntegrationResultRecord record);
}
