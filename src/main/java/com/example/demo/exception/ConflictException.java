package com.example.demo.exception;

public class ConflictException extends IllegalStateException {

    public ConflictException(String message) {
        super(message);
    }
}
