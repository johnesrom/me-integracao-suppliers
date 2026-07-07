package com.exemplo.mesuppliers.features.integrationresult.strategy;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SupplierIntegrationResultStrategy extends AbstractJdbcIntegrationResultStrategy {
    public SupplierIntegrationResultStrategy(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, "supplier", "SOLUS.LEVE_ME_API_SUPPLIER");
    }
}
