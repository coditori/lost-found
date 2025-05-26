package com.example.lostfound.exception;

public class LostItemNotFoundException extends Exception {
    
    public LostItemNotFoundException(String message) {
        super(message);
    }
    
    public LostItemNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
} 