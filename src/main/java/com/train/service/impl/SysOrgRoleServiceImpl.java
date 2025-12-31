package com.train.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.train.entity.SysOrgRole;
import com.train.entity.SysRoleMenu;
import com.train.mapper.SysOrgRoleMapper;
import com.train.mapper.SysRoleMenuMapper;
import com.train.service.ISysOrgRoleService;
import org.springframework.stereotype.Service;

@Service
public class SysOrgRoleServiceImpl  extends ServiceImpl<SysOrgRoleMapper, SysOrgRole> implements ISysOrgRoleService {
}
