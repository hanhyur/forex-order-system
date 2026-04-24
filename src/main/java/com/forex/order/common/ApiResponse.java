package com.forex.order.common;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    private String code;
    private String message;
    private T returnObject;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("OK", "SUCCESS", data);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
