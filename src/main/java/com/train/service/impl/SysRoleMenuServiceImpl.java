package com.train.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.train.entity.SysRole;
import com.train.entity.SysRoleMenu;
import com.train.mapper.SysRoleMapper;
import com.train.mapper.SysRoleMenuMapper;
import com.train.service.SysRoleMenuService;
import org.springframework.stereotype.Service;

@Service
public class SysRoleMenuServiceImpl  extends ServiceImpl<SysRoleMenuMapper, SysRoleMenu> implements SysRoleMenuService {
}
