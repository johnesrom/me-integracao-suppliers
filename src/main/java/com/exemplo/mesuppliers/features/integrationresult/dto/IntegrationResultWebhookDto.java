package com.exemplo.mesuppliers.features.integrationresult.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationResultWebhookDto {

    @NotBlank
    @Size(max = 50)
    @JsonAlias({"topic", "TOPIC"})
    private String topic;

    @NotNull
    @Valid
    @JsonAlias({"data", "DATA"})
    private DataPayload data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataPayload {
        @NotBlank
        @Size(max = 100)
        @JsonAlias({"correlationId", "CORRELATIONID"})
        private String correlationId;

        @NotBlank
        @Size(max = 30)
        @JsonAlias({"resource", "RESOURCE"})
        private String resource;

        @NotNull
        @JsonAlias({"statusCode", "STATUSCODE"})
        private Integer statusCode;

        @Size(max = 30)
        @JsonAlias({"status", "STATUS"})
        private String status;

        @NotBlank
        @Size(max = 100)
        @JsonAlias({"identifier", "IDENTIFIER"})
        private String identifier;

        @Size(max = 200)
        @JsonAlias({"message", "MESSAGE"})
        private String message;
    }
}
