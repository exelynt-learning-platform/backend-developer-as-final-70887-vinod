package com.example.booking.exception;

public class AccessDeniedAppException extends RuntimeException {
    public AccessDeniedAppException(String message) { super(message); }
}
