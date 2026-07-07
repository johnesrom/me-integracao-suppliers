package com.exemplo.mesuppliers.features.integrationresult.strategy;

import com.exemplo.mesuppliers.exception.InvalidWebhookPayloadException;
import com.exemplo.mesuppliers.features.integrationresult.model.IntegrationResultRecord;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

public abstract class AbstractJdbcIntegrationResultStrategy implements IntegrationResultStrategy {

    private static final Logger log = LoggerFactory.getLogger(AbstractJdbcIntegrationResultStrategy.class);

    private final JdbcTemplate jdbcTemplate;
    private final String resource;
    private final String upsertSql;

    protected AbstractJdbcIntegrationResultStrategy(JdbcTemplate jdbcTemplate, String resource, String tableName) {
        this.jdbcTemplate = jdbcTemplate;
        this.resource = resource;
        Map<String, Integer> cols = loadColumnTypes(tableName);
        this.upsertSql = buildUpsertSql(tableName, cols.keySet());
    }

    @Override
    public String resource() { return resource; }

    @Override
    public void persist(IntegrationResultRecord record) {
        BigDecimal idSolus = toRequiredNumber(record.correlationId(), "correlationId");
        String idMe = resolveIdMe(record);
        String statusCode = fit(record.statusCode() == null ? null : String.valueOf(record.statusCode()), 100);
        String status = fit(resolveStatus(record.status(), record.statusCode()), 30);
        String message = fit(record.message(), 200);
        Timestamp sentAt = toTimestamp(record.sentAt());
        Timestamp webhookReceivedAt = toTimestamp(record.webhookReceivedAt());
        Timestamp effectiveSentAt = sentAt != null ? sentAt : webhookReceivedAt;

        jdbcTemplate.update(
                upsertSql,
                new Object[]{idSolus, idMe, statusCode, status, message, effectiveSentAt, webhookReceivedAt},
                new int[]{Types.NUMERIC, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.TIMESTAMP, Types.TIMESTAMP}
        );

        log.info("  Integration result persistido. resource={} idSolus={} idMe={} statusCode={} status={}", resource, idSolus, idMe, statusCode, status);
    }

    private String resolveIdMe(IntegrationResultRecord record) {
        if (record.statusCode() == null || record.statusCode() != 201) return null;
        String identifier = fit(record.identifier(), 100);
        if (!StringUtils.hasText(identifier) || "0".equals(identifier.trim())) return null;
        return identifier;
    }

    private String resolveStatus(String status, Integer statusCode) {
        if (StringUtils.hasText(status)) return status.trim();
        if (statusCode == null) return "PENDING";
        if (statusCode >= 200 && statusCode < 300) return "SUCCESS";
        return "ERROR";
    }

    private BigDecimal toRequiredNumber(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new InvalidWebhookPayloadException("Campo obrigatorio ausente no payload: " + fieldName);
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            throw new InvalidWebhookPayloadException("Campo " + fieldName + " invalido para coluna NUMBER: " + value);
        }
    }

    private String fit(String value, int maxLength) {
        if (!StringUtils.hasText(value)) return null;
        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private Timestamp toTimestamp(java.time.Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private Map<String, Integer> loadColumnTypes(String tableName) {
        Map<String, Integer> columns = jdbcTemplate.execute((Connection connection) -> {
            String sql = "SELECT * FROM " + tableName + " WHERE 1 = 0";
            try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet rs = statement.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                Map<String, Integer> map = new HashMap<>();
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    map.put(metaData.getColumnName(i).trim().toUpperCase(Locale.ROOT), metaData.getColumnType(i));
                }
                return map;
            } catch (SQLException ex) {
                throw new IllegalStateException("Falha ao ler metadados da tabela de integracao: " + tableName, ex);
            }
        });
        if (columns == null || columns.isEmpty() || !columns.containsKey("ID_SOLUS")) {
            throw new IllegalStateException("Tabela de integracao invalida: " + tableName);
        }
        return Map.copyOf(columns);
    }

    private String buildUpsertSql(String tableName, Set<String> columns) {
        List<String> update = new ArrayList<>();
        addUpdateIfColumnExists(columns, update, "ID_ME");
        addUpdateIfColumnExists(columns, update, "STATUS_CODE");
        addUpdateIfColumnExists(columns, update, "STATUS");
        addUpdateIfColumnExists(columns, update, "MESSAGE");
        addUpdateIfColumnExists(columns, update, "SENT_AT");
        addUpdateIfColumnExists(columns, update, "WEBHOOK_RECEIVED_AT");
        if (columns.contains("RESEND")) update.add("target.RESEND = 'N'");

        List<String> insert = new ArrayList<>();
        insert.add("ID_SOLUS");
        addInsertIfColumnExists(columns, insert, "ID_ME");
        addInsertIfColumnExists(columns, insert, "STATUS_CODE");
        addInsertIfColumnExists(columns, insert, "STATUS");
        addInsertIfColumnExists(columns, insert, "MESSAGE");
        addInsertIfColumnExists(columns, insert, "SENT_AT");
        addInsertIfColumnExists(columns, insert, "WEBHOOK_RECEIVED_AT");

        StringBuilder sql = new StringBuilder()
                .append("MERGE INTO ").append(tableName).append(" target\n")
                .append("USING (SELECT ? AS ID_SOLUS, ? AS ID_ME, ? AS STATUS_CODE, ? AS STATUS, ? AS MESSAGE, ? AS SENT_AT, ? AS WEBHOOK_RECEIVED_AT FROM DUAL) source\n")
                .append("ON (target.ID_SOLUS = source.ID_SOLUS)\n");

        if (!update.isEmpty()) {
            sql.append("WHEN MATCHED THEN UPDATE SET\n        ").append(String.join(",\n        ", update)).append("\n");
        }

        sql.append("WHEN NOT MATCHED THEN INSERT (")
                .append(String.join(", ", insert))
                .append(") VALUES (")
                .append(insert.stream().map(c -> "source." + c).collect(Collectors.joining(", ")))
                .append(")");

        return sql.toString();
    }

    private void addUpdateIfColumnExists(Set<String> columns, List<String> updateAssignments, String columnName) {
        if (!columns.contains(columnName)) return;
        if ("SENT_AT".equals(columnName)) {
            updateAssignments.add("target.SENT_AT = COALESCE(target.SENT_AT, source.SENT_AT)");
            return;
        }
        updateAssignments.add("target." + columnName + " = COALESCE(source." + columnName + ", target." + columnName + ")");
    }

    private void addInsertIfColumnExists(Set<String> columns, List<String> insertColumns, String columnName) {
        if (columns.contains(columnName)) insertColumns.add(columnName);
    }
}
