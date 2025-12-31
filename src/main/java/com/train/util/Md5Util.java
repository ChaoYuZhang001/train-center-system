package com.train.util;

import org.apache.commons.codec.digest.DigestUtils;

public class Md5Util {

    // 盐值（生产环境需配置在配置文件中）
    private static final String SALT = "train_center_salt_2025";

    // MD5加密（带盐值）
    public static String encrypt(String password) {
        // 密码 + 盐值 双重加密
        return DigestUtils.md5Hex(password + SALT);
    }

    // 验证密码
    public static boolean verify(String rawPassword, String encryptedPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            return false;
        }
        if (encryptedPassword == null || encryptedPassword.trim().isEmpty()) {
            return false;
        }
        return encrypt(rawPassword).equals(encryptedPassword);
    }
}