package com.woorido.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private String timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, java.time.Instant.now().toString());
    }

    public static <T> ApiResponse<T> error(T data) {
        return new ApiResponse<>(false, data, java.time.Instant.now().toString());
    }
}
