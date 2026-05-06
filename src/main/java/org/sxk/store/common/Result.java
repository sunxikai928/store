package org.sxk.store.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
    private String timestamp;

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data, LocalDateTime.now().toString());
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data, LocalDateTime.now().toString());
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null, LocalDateTime.now().toString());
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null, LocalDateTime.now().toString());
    }

    public static <T> Result<T> badRequest(String message) {
        return new Result<>(400, message, null, LocalDateTime.now().toString());
    }

    public static <T> Result<T> notFound(String message) {
        return new Result<>(404, message, null, LocalDateTime.now().toString());
    }
}