package com.macro.mall.portal.service;

import cn.hutool.core.util.StrUtil;
import com.macro.mall.mapper.UmsAdminMapper;
import com.macro.mall.model.UmsAdmin;
import com.macro.mall.model.UmsAdminExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MiniappAdminService {
    @Autowired
    private UmsAdminMapper adminMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public UmsAdmin authenticate(String username, String password) {
        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            return null;
        }
        UmsAdminExample example = new UmsAdminExample();
        example.createCriteria().andUsernameEqualTo(username);
        List<UmsAdmin> admins = adminMapper.selectByExample(example);
        if (admins == null || admins.size() != 1) {
            return null;
        }
        UmsAdmin admin = admins.get(0);
        if (!Integer.valueOf(1).equals(admin.getStatus())
                || !passwordEncoder.matches(password, admin.getPassword())) {
            return null;
        }
        return admin;
    }
}
