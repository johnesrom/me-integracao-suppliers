package com.exemplo.mesuppliers.exception;

public class MeApiClientException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;

    public MeApiClientException(int statusCode, String responseBody) {
        super("Erro retornado pela API do ME");
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() { return statusCode; }
    public String getResponseBody() { return responseBody; }
}
