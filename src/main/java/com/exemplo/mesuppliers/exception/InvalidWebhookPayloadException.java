package com.exemplo.mesuppliers.exception;

public class InvalidWebhookPayloadException extends RuntimeException {
    public InvalidWebhookPayloadException(String message) { super(message); }
}
