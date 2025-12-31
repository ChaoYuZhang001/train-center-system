package com.train.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 忽略操作日志记录的注解
 */
@Target(ElementType.METHOD) // 只能用于方法
@Retention(RetentionPolicy.RUNTIME) // 运行时保留
public @interface IgnoreLog {
    /**
     * 忽略日志的原因
     */
    String value() default "";
}
