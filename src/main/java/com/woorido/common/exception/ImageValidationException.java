package com.woorido.common.exception;

public class ImageValidationException extends RuntimeException {

  private final String code;
  private final String detail;

  public ImageValidationException(String code, String detail) {
    super(code + ":" + detail);
    this.code = code;
    this.detail = detail;
  }

  public String getCode() {
    return code;
  }

  public String getDetail() {
    return detail;
  }

  public String getApiMessage() {
    return code + ":" + detail;
  }
}
