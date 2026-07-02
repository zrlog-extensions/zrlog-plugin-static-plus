package com.zrlog.plugin.staticplus.controller;

public class StaticPlusApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    public StaticPlusApiResponse() {
    }

    private StaticPlusApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static StaticPlusApiResponse<Void> success() {
        return new StaticPlusApiResponse<Void>(true, null, null);
    }

    public static <T> StaticPlusApiResponse<T> success(T data) {
        return new StaticPlusApiResponse<T>(true, null, data);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
