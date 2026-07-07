package com.exemplo.mesuppliers.features.authentication.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthTokenResponseDto {

    @JsonAlias({"accessToken", "token"})
    private String accessToken;

    @JsonAlias({"expiresIn", "expires_in"})
    private Long expiresIn;

    @JsonAlias({"tokenType", "token_type"})
    private String tokenType;

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public Long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(Long expiresIn) { this.expiresIn = expiresIn; }
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
}
