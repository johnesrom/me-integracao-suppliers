package com.exemplo.mesuppliers.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "me.api")
public class MeApiProperties {

    public enum Environment { STG, PROD }

    @NotNull
    private Environment environment = Environment.STG;
    @NotBlank
    private String stgUrl = "https://stg.api.mercadoe.com";
    @NotBlank
    private String prodUrl = "https://api.mercadoe.com";
    @NotBlank
    private String clientId = "change-me";
    @NotBlank
    private String clientSecret = "change-me";
    @Min(1)
    private int tokenSkewSeconds = 30;

    public Environment getEnvironment() { return environment; }
    public void setEnvironment(Environment environment) { this.environment = environment; }
    public String getStgUrl() { return stgUrl; }
    public void setStgUrl(String stgUrl) { this.stgUrl = stgUrl; }
    public String getProdUrl() { return prodUrl; }
    public void setProdUrl(String prodUrl) { this.prodUrl = prodUrl; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    public int getTokenSkewSeconds() { return tokenSkewSeconds; }
    public void setTokenSkewSeconds(int tokenSkewSeconds) { this.tokenSkewSeconds = tokenSkewSeconds; }

    public String getBaseUrl() {
        return environment == Environment.PROD ? prodUrl : stgUrl;
    }
}
