package com.train.service;

import com.train.entity.SysOrg;
import com.train.exception.BusinessException;
import com.train.mapper.SysOrgMapper;
import com.train.service.impl.SysOrgServiceImpl;
import com.train.util.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class SysOrgServiceImplTest {
    @Mock
    private SysOrgMapper sysOrgMapper;
    @InjectMocks
    private SysOrgServiceImpl orgService;

    @Test
    public void testEditOrg_SysOrgNull() {
        // 覆盖sysOrg == null分支
        assertThrows(BusinessException.class, () -> orgService.editOrg(null));
    }

    @Test
    public void testEditOrg_OrgIdBlank() {
        SysOrg org = new SysOrg();
        org.setOrgId(""); // 覆盖orgId为空分支
        org.setOrgName("测试机构");
        assertThrows(BusinessException.class, () -> orgService.editOrg(org));
    }

    @Test
    public void testEditOrg_OrgNameBlank() {
        SysOrg org = new SysOrg();
        org.setOrgId("1001");
        org.setOrgName(""); // 覆盖orgName为空分支
        assertThrows(BusinessException.class, () -> orgService.editOrg(org));
    }

    @Test
    public void testEditOrg_NameNotUnique() {
        SysOrg org = new SysOrg();
        org.setOrgId("1001");
        org.setOrgName("重复名称");
        // 模拟名称不唯一校验 - 修改为mock sysOrgMapper的查询方法
        when(sysOrgMapper.selectCount(any())).thenReturn(1L); // 假设有重复名称
        assertThrows(BusinessException.class, () -> orgService.editOrg(org));
    }

    @Test
    public void testEditOrg_UpdateFail() {
        SysOrg org = new SysOrg();
        org.setOrgId("1001");
        org.setOrgName("测试机构");
        when(sysOrgMapper.selectCount(any())).thenReturn(0L); // 确保名称唯一
        when(sysOrgMapper.updateById(any())).thenReturn(0); // 覆盖更新失败分支
        assertThrows(BusinessException.class, () -> orgService.editOrg(org));
    }

    @Test
    public void testEditOrg_Success() {
        SysOrg org = new SysOrg();
        org.setOrgId("1001");
        org.setOrgName("测试机构");
        when(sysOrgMapper.selectCount(any())).thenReturn(0L); // 确保名称唯一
        when(sysOrgMapper.updateById(any())).thenReturn(1); // 覆盖更新成功分支
        Result<?> result = orgService.editOrg(org);
        assertEquals("机构编辑成功", result.getMessage());
    }
}
