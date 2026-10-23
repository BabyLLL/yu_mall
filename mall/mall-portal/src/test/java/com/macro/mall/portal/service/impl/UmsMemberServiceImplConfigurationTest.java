package com.macro.mall.portal.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UmsMemberServiceImplConfigurationTest {
    @Test
    void enabledWechatLoginRequiresCredentials() {
        UmsMemberServiceImpl service = new UmsMemberServiceImpl();
        ReflectionTestUtils.setField(service, "wechatEnabled", true);
        ReflectionTestUtils.setField(service, "wechatAppId", "");
        ReflectionTestUtils.setField(service, "wechatAppSecret", "");

        assertThrows(IllegalStateException.class, service::validateWechatConfiguration);
    }

    @Test
    void disabledWechatLoginDoesNotRequireCredentials() {
        UmsMemberServiceImpl service = new UmsMemberServiceImpl();
        ReflectionTestUtils.setField(service, "wechatEnabled", false);
        ReflectionTestUtils.setField(service, "wechatAppId", "");
        ReflectionTestUtils.setField(service, "wechatAppSecret", "");

        assertDoesNotThrow(service::validateWechatConfiguration);
    }
}
