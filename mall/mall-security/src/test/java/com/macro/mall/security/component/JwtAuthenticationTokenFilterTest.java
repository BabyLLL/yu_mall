package com.macro.mall.security.component;

import com.macro.mall.security.service.MiniappSessionService;
import com.macro.mall.security.util.JwtTokenUtil;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationTokenFilterTest {
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private JwtTokenUtil jwtTokenUtil;
    @Mock
    private MiniappSessionService sessionService;
    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationTokenFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationTokenFilter();
        ReflectionTestUtils.setField(filter, "userDetailsService", userDetailsService);
        ReflectionTestUtils.setField(filter, "jwtTokenUtil", jwtTokenUtil);
        ReflectionTestUtils.setField(filter, "miniappSessionService", sessionService);
        ReflectionTestUtils.setField(filter, "expectedAuthType", "member");
        ReflectionTestUtils.setField(filter, "tokenHeader", "Authorization");
        ReflectionTestUtils.setField(filter, "tokenHead", "Bearer ");
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loggedOutSessionIsNotAuthenticated() throws Exception {
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Map<String, Object> claims = memberClaims();
        when(jwtTokenUtil.getClaims("jwt")).thenReturn(claims);
        when(sessionService.isActive("client-1", "session-1", "member", "member-1"))
                .thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(userDetailsService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void administratorTokenIsRejectedByMemberDomain() throws Exception {
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtTokenUtil.getClaims("jwt")).thenReturn(Map.of(
                "clientId", "client-1",
                "sessionId", "session-1",
                "authType", "admin",
                "sub", "admin:admin"));
        // 摊主会话不进入 member 域：session 未激活时不认证，且不会以 member 域逻辑校验
        when(sessionService.isActive("client-1", "session-1", "admin", "admin:admin"))
                .thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void tokenWithoutSessionMetadataIsNotAuthenticated() throws Exception {
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtTokenUtil.getClaims("jwt")).thenReturn(Map.of(
                "clientId", "client-1",
                "authType", "member",
                "sub", "member-1"));

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(sessionService).isActive("client-1", null, "member", "member-1");
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void expiredTokenIsNotAuthenticatedEvenWithActiveSession() throws Exception {
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();
        UserDetails details = User.withUsername("member-1").password("password").authorities("USER").build();
        when(jwtTokenUtil.getClaims("jwt")).thenReturn(memberClaims());
        when(sessionService.isActive("client-1", "session-1", "member", "member-1"))
                .thenReturn(true);
        when(jwtTokenUtil.getUserNameFromToken("jwt")).thenReturn("member-1");
        when(userDetailsService.loadUserByUsername("member-1")).thenReturn(details);
        when(jwtTokenUtil.validateToken("jwt", details)).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void activeMemberSessionIsAuthenticated() throws Exception {
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();
        UserDetails details = User.withUsername("member-1").password("password").authorities("USER").build();
        when(jwtTokenUtil.getClaims("jwt")).thenReturn(memberClaims());
        when(sessionService.isActive("client-1", "session-1", "member", "member-1"))
                .thenReturn(true);
        when(jwtTokenUtil.getUserNameFromToken("jwt")).thenReturn("member-1");
        when(userDetailsService.loadUserByUsername("member-1")).thenReturn(details);
        when(jwtTokenUtil.validateToken("jwt", details)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer jwt");
        request.addHeader("source-client", "miniapp");
        return request;
    }

    private Map<String, Object> memberClaims() {
        return Map.of(
                "clientId", "client-1",
                "sessionId", "session-1",
                "authType", "member",
                "sub", "member-1");
    }
}
