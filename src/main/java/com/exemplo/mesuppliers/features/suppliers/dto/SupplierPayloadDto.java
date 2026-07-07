package com.exemplo.mesuppliers.features.suppliers.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import lombok.Data;

@Data
public class SupplierPayloadDto {
    @JsonIgnore
    private String meId;
    @JsonIgnore
    private String syncAction;

    private String clientSupplierId;
    private String tradingName;
    private String companyName;
    private String contact;
    private String email;
    private String address;
    private String complement;
    private String city;
    private String country;
    private String postalCode;
    private String phoneNumber;
    private Boolean isDeactivated;
    private String stateRegistrationNumber;
    private Integer languageId;
    private String iso3166CountryCode;
    private String documentNumber;
    private String documentType;
    private List<AdditionalEmailDto> additionalEmails;

    @Data
    public static class AdditionalEmailDto {
        private String email;
    }
}
