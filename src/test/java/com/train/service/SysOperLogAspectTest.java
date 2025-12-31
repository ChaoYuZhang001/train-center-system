package com.train.service;
import com.train.aspect.SysOperLogAspect;
import com.train.entity.SysOrg;
import com.train.entity.SysRole;
import com.train.entity.SysUser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SysOperLogAspectTest {
    private final SysOperLogAspect aspect = new SysOperLogAspect();

    @Test
    public void testGetOperObjName_SysOrg() {
        SysOrg org = new SysOrg();
        org.setOrgName("测试机构");
        String name = aspect.getOperObjName(new Object[]{org});
        assertEquals("测试机构", name);
    }

    @Test
    public void testGetOperObjName_SysRole() {
        SysRole role = new SysRole();
        role.setRoleName("管理员");
        String name = aspect.getOperObjName(new Object[]{role});
        assertEquals("管理员", name);
    }

    @Test
    public void testGetOperObjName_SysUser() {
        SysUser user = new SysUser();
        user.setUserName("测试用户");
        String name = aspect.getOperObjName(new Object[]{user});
        assertEquals("测试用户", name);
    }

    @Test
    public void testGetOperObjName_OtherType() {
        // 非指定类型参数
        String name = aspect.getOperObjName(new Object[]{new Object()});
        assertEquals("", name);
    }

    @Test
    public void testGetOperObjName_NullArgs() {
        // 参数为null
        String name = aspect.getOperObjName(null);
        assertEquals("", name);
    }

    @Test
    public void testGetOperObjName_ArgsContainNull() {
        // 参数含null
        String name = aspect.getOperObjName(new Object[]{null, new SysUser()});
        assertEquals("", name); // 第二个参数是SysUser但未设userName
    }
}