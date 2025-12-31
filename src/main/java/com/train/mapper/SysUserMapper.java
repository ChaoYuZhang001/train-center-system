package com.train.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.train.entity.SysUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysUserMapper extends BaseMapper<SysUser> {

    // 用户模糊查询（支持账号、用户名）
    IPage<SysUser> selectUserByLike(Page<SysUser> page,
                                    @Param("account") String account,
                                    @Param("userName") String userName);

    // 校验账号唯一性
    Integer checkAccountUnique(@Param("account") String account);

    // 校验用户名唯一性（机构内）
    Integer checkUserNameUnique(@Param("userName") String userName,
                                @Param("orgId") String orgId);

    // 根据用户ID查询关联的角色ID列表
    List<String> selectRoleIdsByUserId(@Param("userId") Long userId);

    // 根据账号查询用户（登录用）
    SysUser selectUserByAccount(@Param("account") String account, @Param("orgId") String orgId);
    /**
     * 根据用户账号查询用户所有权限标识
     * @param account 用户账号
     * @return 权限标识列表
     */
    List<String> selectUserPermsByAccount(@Param("account") String account);
}