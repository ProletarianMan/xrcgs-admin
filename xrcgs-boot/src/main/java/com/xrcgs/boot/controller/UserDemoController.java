package com.xrcgs.boot.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xrcgs.iam.entity.SysUser;
import com.xrcgs.iam.mapper.SysUserMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户列表测试
 */
@RestController
public class UserDemoController {

    private final SysUserMapper userMapper;
    public UserDemoController(SysUserMapper userMapper){ this.userMapper = userMapper; }

    @GetMapping("/api/iam/users")
    public List<SysUser> list() {
        return userMapper.selectList(Wrappers.emptyWrapper());
    }
}
