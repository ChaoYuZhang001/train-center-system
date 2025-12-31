package com.train.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.train.entity.SysOrg;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 机构Mapper接口
 * 继承MyBatis-Plus BaseMapper，实现基础CRUD
 */
@Mapper
public interface SysOrgMapper extends BaseMapper<SysOrg> {
    // 机构模糊查询（支持名称、管理员账号、管理员姓名）
    IPage<SysOrg> selectOrgByLike(Page<SysOrg> page,
                                  @Param("orgName") String orgName,
                                  @Param("adminAccount") String adminAccount,
                                  @Param("adminName") String adminName);

    // 校验机构名称唯一性
    Integer checkOrgNameUnique(@Param("orgName") String orgName);

    // 校验管理员账号唯一性
    Integer checkAdminAccountUnique(@Param("adminAccount") String adminAccount);
}