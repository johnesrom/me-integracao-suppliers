package com.exemplo.mesuppliers.features.integrationresult.service;

import com.exemplo.mesuppliers.exception.InvalidWebhookPayloadException;
import com.exemplo.mesuppliers.features.integrationresult.model.IntegrationResultRecord;
import com.exemplo.mesuppliers.features.integrationresult.strategy.IntegrationResultStrategy;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class IntegrationResultPersistenceServiceImpl implements IntegrationResultPersistenceService {

    private final Map<String, IntegrationResultStrategy> strategyByResource;

    public IntegrationResultPersistenceServiceImpl(List<IntegrationResultStrategy> strategies) {
        Map<String, IntegrationResultStrategy> map = new HashMap<>();
        for (IntegrationResultStrategy strategy : strategies) {
            String resource = normalize(strategy.resource());
            if (map.putIfAbsent(resource, strategy) != null) {
                throw new IllegalStateException("Strategy duplicada para resource: " + resource);
            }
        }
        this.strategyByResource = Map.copyOf(map);
    }

    @Override
    public void persist(String resource, IntegrationResultRecord record) {
        IntegrationResultStrategy strategy = strategyByResource.get(normalize(resource));
        if (strategy == null) throw new InvalidWebhookPayloadException("Resource nao suportado: " + resource);
        strategy.persist(record);
    }

    @Override
    public boolean supports(String resource) {
        return strategyByResource.containsKey(normalize(resource));
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) return "";
        String n = value.trim().toLowerCase(Locale.ROOT);
        return switch (n) {
            case "suppliers", "supplier" -> "supplier";
            default -> n;
        };
    }
}
