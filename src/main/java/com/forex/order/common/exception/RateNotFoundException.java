package com.forex.order.common.exception;

public class RateNotFoundException extends RuntimeException {

    public RateNotFoundException(String message) {
        super(message);
    }
}
