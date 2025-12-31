package com.train.util;

import com.train.constant.ResultConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "统一响应结果")
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "响应码")
    private int code;

    @Schema(description = "响应消息")
    private String message;

    @Schema(description = "响应数据")
    private T data;

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // 成功响应（无数据）
    public static <T> Result<T> success() {
        return new Result<>(ResultConstant.SUCCESS_CODE, ResultConstant.SUCCESS_MSG, null);
    }

    // 成功响应（带数据）
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultConstant.SUCCESS_CODE, ResultConstant.SUCCESS_MSG, data);
    }

    // 成功响应（自定义消息）
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultConstant.SUCCESS_CODE, message, data);
    }

    // 失败响应（默认消息）
    public static <T> Result<T> error() {
        return new Result<>(ResultConstant.ERROR_CODE, ResultConstant.ERROR_MSG, null);
    }

    // 失败响应（自定义消息）
    public static <T> Result<T> error(String message) {
        return new Result<>(ResultConstant.ERROR_CODE, message, null);
    }

    // 失败响应（自定义状态码和消息）
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    // 无权限响应
    public static <T> Result<T> noAuth() {
        return new Result<>(ResultConstant.NO_AUTH_CODE, ResultConstant.NO_AUTH_MSG, null);
    }

    // 资源不存在响应
    public static <T> Result<T> notFound() {
        return new Result<>(ResultConstant.NOT_FOUND_CODE, ResultConstant.NOT_FOUND_MSG, null);
    }
}
