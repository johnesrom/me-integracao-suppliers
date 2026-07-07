package com.exemplo.mesuppliers.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidWebhookPayloadException.class)
    public ResponseEntity<?> handleInvalidWebhookPayloadException(InvalidWebhookPayloadException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(MeApiClientException.class)
    public ResponseEntity<?> handleMeApiClientException(MeApiClientException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode());
        String responseBody = ex.getResponseBody();

        if (!StringUtils.hasText(responseBody)) {
            return ResponseEntity.status(status == null ? HttpStatus.BAD_GATEWAY : status)
                    .body(Map.of("error", "Erro retornado pela API do ME"));
        }

        MediaType contentType = looksLikeJson(responseBody) ? MediaType.APPLICATION_JSON : MediaType.TEXT_PLAIN;
        return ResponseEntity.status(status == null ? HttpStatus.BAD_GATEWAY : status)
                .contentType(contentType)
                .body(responseBody);
    }

    private boolean looksLikeJson(String content) {
        String value = content.trim();
        return (value.startsWith("{") && value.endsWith("}")) || (value.startsWith("[") && value.endsWith("]"));
    }
}
