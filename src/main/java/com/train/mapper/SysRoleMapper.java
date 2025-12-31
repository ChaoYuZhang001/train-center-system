package com.train.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.train.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {
    // 角色模糊查询（支持角色名称、创建人姓名）
    IPage<SysRole> selectRoleByLike(Page<SysRole> page,
                                    @Param("roleName") String roleName,
                                    @Param("createBy") String createBy);

    // 校验同一机构内角色名称唯一性
    Integer checkRoleNameUnique(@Param("roleName") String roleName,
                                @Param("orgId") String orgId);

    // 根据角色ID查询关联的菜单ID列表
    List<Long> selectMenuIdsByRoleId(@Param("roleId") String roleId);
}