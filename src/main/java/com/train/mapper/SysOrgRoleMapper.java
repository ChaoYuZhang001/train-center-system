package com.train.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.train.entity.SysOrgRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 机构-角色关联Mapper接口
 * 继承MyBatis-Plus BaseMapper，实现基础CRUD操作
 * 无需手动编写XML，直接与业务层联动
 */
@Mapper
public interface SysOrgRoleMapper extends BaseMapper<SysOrgRole> {

    List<String> selectOrgIdsByRoleId(@Param("roleId") String roleId);

    List<String> selectRoleIdsByOrgIds(@Param("orgIds") List<String> orgIds);

    List<String> selectRoleIdsByOrgId(@Param("orgId") String currentOrgId);
}