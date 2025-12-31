package com.train.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * 配置化AES加密解密工具类（使用固定密钥）
 */
@Component
@Slf4j
public class AESUtil {

    private static String KEY = "1234567887654321";

    private static String IV = KEY;

    public static String encrypt(String data, String key, String iv) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            int blockSize = cipher.getBlockSize();
            byte[] dataBytes = data.getBytes();
            int plaintextLength = dataBytes.length;
            if (plaintextLength % blockSize != 0) {
                plaintextLength += blockSize - plaintextLength % blockSize;
            }

            byte[] plaintext = new byte[plaintextLength];
            System.arraycopy(dataBytes, 0, plaintext, 0, dataBytes.length);
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes());
            cipher.init(1, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(plaintext);
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception var11) {
            log.error("encrypt", var11);
            return null;
        }
    }

    public static String desEncrypt(String data, String key, String iv) {
        try {
            byte[] encrypted1 = Base64.getDecoder().decode(data);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes());
            cipher.init(2, keySpec, ivSpec);
            byte[] original = cipher.doFinal(encrypted1);
            return new String(original).trim();
        } catch (Exception var8) {
            log.error("desEncrypt", var8);
            return null;
        }
    }

    public static String encrypt(String data) {
        return encrypt(data, KEY, IV);
    }

    public static String desEncrypt(String data) {
        return desEncrypt(data, KEY, IV);
    }

    public static void main(String[] args) throws Exception {
        String encrypt = AESUtil.encrypt("bcd74a59a8294593e7946253ff9fd386");
        System.out.println("加密结果: " + encrypt);
        String decrypt = AESUtil.desEncrypt("h704/1LvkG555m+12IZcMLfgl6FqcJlRKvyWQWr3yvA=");
        System.out.println("解密结果: " + decrypt);
    }
}
