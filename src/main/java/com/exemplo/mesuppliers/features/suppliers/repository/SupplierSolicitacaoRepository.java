package com.exemplo.mesuppliers.features.suppliers.repository;

import com.exemplo.mesuppliers.features.suppliers.dto.SupplierPayloadDto;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SupplierSolicitacaoRepository {

    private static final Logger log = LoggerFactory.getLogger(SupplierSolicitacaoRepository.class);

    private static final String SQL = """
            SELECT
                TMP.ID_ME AS \"meId\",
                CASE
                    WHEN TMP.ID_SOLUS IS NULL THEN 'CREATE'
                    WHEN TMP.RESEND = 'S'
                         AND TRIM(TMP.STATUS_CODE) IN ('201', '200')
                         AND TMP.ID_ME IS NOT NULL THEN 'UPDATE'
                    WHEN TMP.RESEND = 'S'
                         AND (
                             TMP.STATUS_CODE IS NULL
                             OR TRIM(TMP.STATUS_CODE) NOT IN ('201', '200')
                         ) THEN 'CREATE'
                END AS \"syncAction\",
                V.CLIENTSUPPLIERID AS \"clientSupplierId\",
                V.TRADINGNAME AS \"tradingName\",
                V.COMPANYNAME AS \"companyName\",
                V.CONTACT AS \"contact\",
                V.EMAIL AS \"email\",
                V.ADDRESS AS \"address\",
                V.COMPLEMENT AS \"complement\",
                V.CITY AS \"city\",
                V.COUNTRY AS \"country\",
                V.POSTALCODE AS \"postalCode\",
                V.PHONENUMBER AS \"phoneNumber\",
                V.ISDEACTIVATED AS \"isDeactivated\",
                V.STATEREGISTRATIONNUMBER AS \"stateRegistrationNumber\",
                V.LANGUAGEID AS \"languageId\",
                V.DOCUMENTNUMBER AS \"documentNumber\",
                V.DOCUMENTTYPE AS \"documentType\",
                V.ISO3166COUNTRYCODE AS \"iso3166CountryCode\"
            FROM SOLUS.VW_LEVE_ME_SUPPLIER V
            LEFT JOIN SOLUS.LEVE_ME_API_SUPPLIER TMP
                ON TMP.ID_SOLUS = V.CLIENTSUPPLIERID
            WHERE TMP.ID_SOLUS IS NULL OR TMP.RESEND = 'S'
            AND ROWNUM <= 400
            """;

    private final JdbcTemplate jdbcTemplate;

    public SupplierSolicitacaoRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public List<SupplierPayloadDto> findAllForSync() {
        log.info("Lendo suppliers para sync.");
        return jdbcTemplate.query(SQL, (rs, rowNum) -> {
            SupplierPayloadDto dto = new SupplierPayloadDto();
            dto.setMeId(rs.getString("meId"));
            dto.setSyncAction(rs.getString("syncAction"));
            dto.setClientSupplierId(rs.getString("clientSupplierId"));
            dto.setTradingName(rs.getString("tradingName"));
            dto.setCompanyName(rs.getString("companyName"));
            dto.setContact(rs.getString("contact"));
            dto.setEmail(rs.getString("email"));
            dto.setAddress(rs.getString("address"));
            dto.setComplement(rs.getString("complement"));
            dto.setCity(rs.getString("city"));
            dto.setCountry(rs.getString("country"));
            dto.setPostalCode(rs.getString("postalCode"));
            dto.setPhoneNumber(rs.getString("phoneNumber"));
            dto.setIsDeactivated(toBoolean(rs.getObject("isDeactivated")));
            dto.setStateRegistrationNumber(rs.getString("stateRegistrationNumber"));
            dto.setLanguageId(rs.getInt("languageId"));
            dto.setDocumentNumber(rs.getString("documentNumber"));
            dto.setDocumentType(rs.getString("documentType"));
            dto.setIso3166CountryCode(rs.getString("iso3166CountryCode"));
            return dto;
        });
    }

    private Boolean toBoolean(Object value) {
        if (value == null) return null;
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.intValue() != 0;
        String t = value.toString().trim();
        if ("1".equals(t) || "Y".equalsIgnoreCase(t) || "S".equalsIgnoreCase(t) || "TRUE".equalsIgnoreCase(t)) return Boolean.TRUE;
        if ("0".equals(t) || "N".equalsIgnoreCase(t) || "FALSE".equalsIgnoreCase(t)) return Boolean.FALSE;
        return Boolean.parseBoolean(t);
    }
}
