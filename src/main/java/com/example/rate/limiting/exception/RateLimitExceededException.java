package com.example.rate.limiting.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}
