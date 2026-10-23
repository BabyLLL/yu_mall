package com.macro.mall.security.component;

import com.macro.mall.security.util.JwtTokenUtil;
import com.macro.mall.security.service.MiniappSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * JWT登录授权过滤器
 * Created by macro on 2018/4/26.
 */
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationTokenFilter.class);
    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private MiniappSessionService miniappSessionService;
    @Value("${miniapp.auth.expected-type:}")
    private String expectedAuthType;
    @Value("${jwt.tokenHeader}")
    private String tokenHeader;
    @Value("${jwt.tokenHead}")
    private String tokenHead;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String authHeader = request.getHeader(this.tokenHeader);
        if (authHeader != null && authHeader.startsWith(this.tokenHead)) {
            String authToken = authHeader.substring(this.tokenHead.length());// The part after "Bearer "
            Map<String, Object> claims = jwtTokenUtil.getClaims(authToken);
            boolean miniappToken = claims != null && claims.get("clientId") != null;
            // 摊主会话（authType=admin）不参与 expected-type 域检查，由接口层的 MINIAPP_ADMIN 权限控制
            boolean adminSession = claims != null
                    && MiniappSessionService.ADMIN.equals(stringValue(claims.get("authType")));
            if ((miniappToken || "miniapp".equalsIgnoreCase(request.getHeader("source-client")))
                    && (adminSession ? !isAdminSessionActive(claims) : !isActiveMiniappSession(authToken))) {
                chain.doFilter(request, response);
                return;
            }
            String username = jwtTokenUtil.getUserNameFromToken(authToken);
            LOGGER.info("checking username:{}", username);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = buildUserDetails(authToken, username, adminSession);
                if (userDetails != null && jwtTokenUtil.validateToken(authToken, userDetails)) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    LOGGER.info("authenticated user:{}", username);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * 构造认证主体：摊主会话（authType=admin）的身份只存在于 token 中，
     * 不能经 userDetailsService 查会员表（必然查不到），直接从 subject 构造；
     * 其余会话仍走 userDetailsService。
     */
    private UserDetails buildUserDetails(String authToken, String username, boolean adminSession) {
        if (!adminSession) {
            return this.userDetailsService.loadUserByUsername(username);
        }
        return org.springframework.security.core.userdetails.User.withUsername(username)
                .password("")
                .authorities(MiniappSessionService.AUTHORITY_MINIAPP_ADMIN)
                .build();
    }

    private boolean isAdminSessionActive(Map<String, Object> claims) {
        return miniappSessionService.isActive(
                stringValue(claims.get("clientId")),
                stringValue(claims.get("sessionId")),
                MiniappSessionService.ADMIN,
                stringValue(claims.get("sub")));
    }

    private boolean isActiveMiniappSession(String token) {
        Map<String, Object> claims = jwtTokenUtil.getClaims(token);
        if (claims == null) {
            return false;
        }
        String authType = stringValue(claims.get("authType"));
        if (expectedAuthType != null && !expectedAuthType.isBlank() && !expectedAuthType.equals(authType)) {
            return false;
        }
        return miniappSessionService.isActive(
                stringValue(claims.get("clientId")),
                stringValue(claims.get("sessionId")),
                authType,
                stringValue(claims.get("sub")));
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
