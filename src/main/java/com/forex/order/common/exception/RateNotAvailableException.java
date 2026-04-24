package com.forex.order.common.exception;

public class RateNotAvailableException extends RuntimeException {

    public RateNotAvailableException(String message) {
        super(message);
    }
}
