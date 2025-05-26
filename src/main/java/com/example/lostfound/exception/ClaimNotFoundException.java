package com.example.lostfound.exception;

public class ClaimNotFoundException extends Exception {
    
    public ClaimNotFoundException(String message) {
        super(message);
    }
    
    public ClaimNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
} 