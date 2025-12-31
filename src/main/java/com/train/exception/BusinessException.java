package com.train.exception;

import com.train.constant.ResultConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private int code;
    private String message;

    public BusinessException(String message) {
        super(message);
        this.code = ResultConstant.ERROR_CODE;
        this.message = message;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}