package com.macro.mall.portal.controller;

import com.macro.mall.common.api.CommonResult;
import com.macro.mall.model.UmsAdmin;
import com.macro.mall.portal.dto.MiniappAdminLoginParam;
import com.macro.mall.portal.service.MiniappAdminService;
import com.macro.mall.portal.service.UmsMemberService;
import com.macro.mall.security.model.MiniappSession;
import com.macro.mall.security.service.MiniappSessionService;
import com.macro.mall.security.util.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UmsMemberControllerTest {
    @Mock
    private UmsMemberService memberService;
    @Mock
    private MiniappAdminService adminService;
    @Mock
    private JwtTokenUtil jwtTokenUtil;
    @Mock
    private MiniappSessionService sessionService;

    private UmsMemberController controller;

    @BeforeEach
    void setUp() {
        controller = new UmsMemberController();
        ReflectionTestUtils.setField(controller, "memberService", memberService);
        ReflectionTestUtils.setField(controller, "miniappAdminService", adminService);
        ReflectionTestUtils.setField(controller, "jwtTokenUtil", jwtTokenUtil);
        ReflectionTestUtils.setField(controller, "miniappSessionService", sessionService);
        ReflectionTestUtils.setField(controller, "tokenHead", "Bearer ");
    }

    @Test
    void activeSessionRejectsAdminLoginBeforeCredentialValidation() {
        when(sessionService.hasActiveSession("client-1")).thenReturn(true);

        CommonResult<?> result = controller.miniappAdminLogin(
                loginParam("admin", "secret"), "miniapp", "client-1");

        assertEquals(409, result.getCode());
        verify(adminService, never()).authenticate(any(), any());
    }

    @Test
    void portalIssuesAdministratorMiniappSession() {
        UmsAdmin admin = new UmsAdmin();
        admin.setUsername("admin");
        admin.setPassword("encoded");
        admin.setStatus(1);
        MiniappSession session = new MiniappSession("client-1", "session-1", "admin", "admin:admin");
        when(adminService.authenticate("admin", "secret")).thenReturn(admin);
        when(sessionService.activateIfAbsent("client-1", "admin", "admin:admin")).thenReturn(session);
        when(jwtTokenUtil.generateToken(any(), any())).thenReturn("jwt");

        CommonResult<?> result = controller.miniappAdminLogin(
                loginParam("admin", "secret"), "miniapp", "client-1");

        assertEquals(200, result.getCode());
        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertEquals("admin", data.get("authType"));
        assertEquals("session-1", data.get("sessionId"));
    }

    @Test
    void mismatchedLogoutPreservesServerSession() {
        when(jwtTokenUtil.getClaims("jwt")).thenReturn(Map.of(
                "clientId", "client-1",
                "sessionId", "session-1",
                "authType", "member",
                "sub", "member-1"));
        when(sessionService.invalidateIfActive(
                "client-1", "session-1", "member", "member-1")).thenReturn(false);

        CommonResult<?> result = controller.miniappLogout("client-1", "Bearer jwt");

        assertEquals(40901, result.getCode());
        verify(sessionService).invalidateIfActive(
                eq("client-1"), eq("session-1"), eq("member"), eq("member-1"));
    }

    private MiniappAdminLoginParam loginParam(String username, String password) {
        MiniappAdminLoginParam param = new MiniappAdminLoginParam();
        param.setUsername(username);
        param.setPassword(password);
        return param;
    }
}
