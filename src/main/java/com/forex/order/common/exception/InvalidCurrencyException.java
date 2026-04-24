package com.forex.order.common.exception;

public class InvalidCurrencyException extends RuntimeException {

    public InvalidCurrencyException(String message) {
        super(message);
    }
}
