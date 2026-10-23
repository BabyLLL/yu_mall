package com.macro.mall.portal.dao;

import com.macro.mall.portal.domain.UmsMemberWechatIdentity;
import org.apache.ibatis.annotations.Param;

public interface UmsMemberWechatIdentityDao {
    UmsMemberWechatIdentity findByAppIdAndOpenId(@Param("appId") String appId, @Param("openId") String openId);

    int insert(UmsMemberWechatIdentity identity);

    int updateMemberProfile(@Param("id") Long id, @Param("memberId") Long memberId);
}
