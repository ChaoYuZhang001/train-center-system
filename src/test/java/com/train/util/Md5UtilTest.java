package com.train.util;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class Md5UtilTest {
    // 假设encrypt方法存在（补充测试依赖的私有/公有方法）
    @Test
    public void testEncrypt() {
        String rawPwd = "a123456";
        String encrypted = Md5Util.encrypt(rawPwd);
        assertNotNull(encrypted);
        assertNotEquals(rawPwd, encrypted);
    }

    @Test
    public void testVerify_Success() {
        String rawPwd = "Test@123";
        String encrypted = Md5Util.encrypt(rawPwd);
        assertTrue(Md5Util.verify(rawPwd, encrypted));
    }

    @Test
    public void testVerify_Fail() {
        String rawPwd = "Test@123";
        String encrypted = Md5Util.encrypt(rawPwd);
        assertFalse(Md5Util.verify("Wrong@456", encrypted));
    }

    @Test
    public void testVerify_Null() {
        // 覆盖空值处理（若encrypt支持）
        assertThrows(NullPointerException.class, () -> Md5Util.verify(null, "abc"));
        assertThrows(NullPointerException.class, () -> Md5Util.verify("abc", null));
    }
}