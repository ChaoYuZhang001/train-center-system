package com.train.annotation;

import java.lang.annotation.*;

/**
 * 自定义权限注解，用于接口权限控制
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {
    // 权限标识（与sys_menu表的perms字段对应）
    String value() default "";
}