package com.woorido.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {

  private boolean success;
  private T data;
  private String message;
  private String timestamp;

  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(true, data, null, java.time.Instant.now().toString());
  }

  public static <T> ApiResponse<T> success(T data, String message) {
    return new ApiResponse<>(true, data, message, java.time.Instant.now().toString());
  }

  public static <T> ApiResponse<T> error(String message) {
    return new ApiResponse<>(false, null, message, java.time.Instant.now().toString());
  }
}
