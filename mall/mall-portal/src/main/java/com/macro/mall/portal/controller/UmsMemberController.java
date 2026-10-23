package com.macro.mall.portal.controller;

import com.macro.mall.common.api.CommonResult;
import com.macro.mall.common.api.ResultCode;
import com.macro.mall.model.UmsAdmin;
import com.macro.mall.model.UmsMember;
import com.macro.mall.portal.dto.MiniappAdminLoginParam;
import com.macro.mall.portal.service.MiniappAdminService;
import com.macro.mall.portal.service.UmsMemberService;
import com.macro.mall.portal.domain.MemberDetails;
import com.macro.mall.security.service.MiniappSessionService;
import com.macro.mall.security.model.MiniappSession;
import com.macro.mall.security.util.JwtTokenUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Map;
import java.util.HashMap;

/**
 * 会员管理Controller
 * Created by macro on 2018/8/3.
 */
@Controller
@Tag(name = "UmsMemberController", description = "会员登录注册管理")
@RequestMapping("/sso")
public class UmsMemberController {
    @Value("${jwt.tokenHeader}")
    private String tokenHeader;
    @Value("${jwt.tokenHead}")
    private String tokenHead;
    @Autowired
    private UmsMemberService memberService;
    @Autowired
    private MiniappAdminService miniappAdminService;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private MiniappSessionService miniappSessionService;

    @Operation(summary = "会员注册")
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult register(@RequestParam String username,
                                 @RequestParam String password,
                                 @RequestParam String telephone,
                                 @RequestParam String authCode) {
        memberService.register(username, password, telephone, authCode);
        return CommonResult.success(null,"注册成功");
    }

    @Operation(summary = "会员登录")
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult login(@RequestParam String username,
                              @RequestParam String password,
                              @RequestHeader(value = "source-client", required = false) String sourceClient) {
        if ("miniapp".equalsIgnoreCase(sourceClient)) {
            return CommonResult.forbidden(null);
        }
        String token = memberService.login(username, password);
        if (token == null) {
            return CommonResult.validateFailed("用户名或密码错误");
        }
        Map<String, String> tokenMap = new HashMap<>();
        tokenMap.put("token", token);
        tokenMap.put("tokenHead", tokenHead);
        return CommonResult.success(tokenMap);
    }

    @Operation(summary = "微信小程序登录")
    @RequestMapping(value = "/wechat/login", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult wechatLogin(@RequestParam String code,
                                    @RequestParam String clientId,
                                    @RequestParam(required = false) String nickname,
                                    @RequestParam(required = false) String avatarUrl) {
        if (miniappSessionService.hasActiveSession(clientId)) {
            return CommonResult.failed(ResultCode.LOGIN_CONFLICT);
        }
        MemberDetails details = (MemberDetails) memberService.loginByWechat(code, clientId, nickname, avatarUrl);
        MiniappSession session = miniappSessionService.activateIfAbsent(
                clientId, MiniappSessionService.MEMBER, details.getUsername());
        if (session == null) {
            return CommonResult.failed(ResultCode.LOGIN_CONFLICT);
        }
        Map<String, Object> claims = new HashMap<>();
        claims.put("clientId", clientId);
        claims.put("sessionId", session.getSessionId());
        claims.put("authType", MiniappSessionService.MEMBER);
        String token = jwtTokenUtil.generateToken(details, claims);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("tokenHead", tokenHead);
        data.put("authType", MiniappSessionService.MEMBER);
        data.put("clientId", clientId);
        data.put("sessionId", session.getSessionId());
        data.put("member", details.getUmsMember());
        return CommonResult.success(data);
    }

    @Operation(summary = "小程序管理员登录")
    @RequestMapping(value = "/miniapp/admin/login", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult miniappAdminLogin(@Validated @RequestBody MiniappAdminLoginParam loginParam,
                                          @RequestHeader(value = "source-client", required = false) String sourceClient,
                                          @RequestHeader(value = "X-Miniapp-Client-Id", required = false) String clientId) {
        if (!"miniapp".equalsIgnoreCase(sourceClient) || clientId == null || clientId.isBlank()) {
            return CommonResult.forbidden(null);
        }
        if (miniappSessionService.hasActiveSession(clientId)) {
            return CommonResult.failed(ResultCode.LOGIN_CONFLICT);
        }
        UmsAdmin admin = miniappAdminService.authenticate(loginParam.getUsername(), loginParam.getPassword());
        if (admin == null) {
            return CommonResult.validateFailed("用户名或密码错误");
        }
        String subject = MiniappSessionService.ADMIN + ":" + admin.getUsername();
        UserDetails details = User.withUsername(subject)
                .password(admin.getPassword())
                .authorities("MINIAPP_ADMIN")
                .build();
        MiniappSession session = miniappSessionService.activateIfAbsent(
                clientId, MiniappSessionService.ADMIN, subject);
        if (session == null) {
            return CommonResult.failed(ResultCode.LOGIN_CONFLICT);
        }
        Map<String, Object> claims = new HashMap<>();
        claims.put("clientId", clientId);
        claims.put("sessionId", session.getSessionId());
        claims.put("authType", MiniappSessionService.ADMIN);
        String token = jwtTokenUtil.generateToken(details, claims);
        Map<String, Object> profile = new HashMap<>();
        profile.put("username", admin.getUsername());
        profile.put("icon", admin.getIcon());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("tokenHead", tokenHead);
        data.put("authType", MiniappSessionService.ADMIN);
        data.put("clientId", clientId);
        data.put("sessionId", session.getSessionId());
        data.put("admin", profile);
        return CommonResult.success(data);
    }

    @Operation(summary = "注销小程序活动会话")
    @RequestMapping(value = "/miniapp/logout", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult miniappLogout(@RequestHeader("X-Miniapp-Client-Id") String clientId,
                                      @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith(tokenHead)) {
            return CommonResult.unauthorized(null);
        }
        Map<String, Object> claims = jwtTokenUtil.getClaims(authorization.substring(tokenHead.length()));
        if (claims == null || !clientId.equals(String.valueOf(claims.get("clientId")))) {
            return CommonResult.unauthorized(null);
        }
        String authType = String.valueOf(claims.get("authType"));
        if (!MiniappSessionService.MEMBER.equals(authType) && !MiniappSessionService.ADMIN.equals(authType)) {
            return CommonResult.unauthorized(null);
        }
        boolean active = miniappSessionService.invalidateIfActive(clientId,
                String.valueOf(claims.get("sessionId")), authType,
                String.valueOf(claims.get("sub")));
        if (!active) {
            return CommonResult.failed(ResultCode.SESSION_MISMATCH);
        }
        return CommonResult.success(null);
    }

    @Operation(summary = "获取会员信息")
    @RequestMapping(value = "/info", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult info(Principal principal) {
        if(principal==null){
            return CommonResult.unauthorized(null);
        }
        UmsMember member = memberService.getCurrentMember();
        return CommonResult.success(member);
    }

    @Operation(summary = "获取验证码")
    @RequestMapping(value = "/getAuthCode", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult getAuthCode(@RequestParam String telephone) {
        String authCode = memberService.generateAuthCode(telephone);
        return CommonResult.success(authCode,"获取验证码成功");
    }

    @Operation(summary = "会员修改密码")
    @RequestMapping(value = "/updatePassword", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult updatePassword(@RequestParam String telephone,
                                 @RequestParam String password,
                                 @RequestParam String authCode) {
        memberService.updatePassword(telephone,password,authCode);
        return CommonResult.success(null,"密码修改成功");
    }


    @Operation(summary = "刷新token")
    @RequestMapping(value = "/refreshToken", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult refreshToken(HttpServletRequest request) {
        String token = request.getHeader(tokenHeader);
        String refreshToken = memberService.refreshToken(token);
        if (refreshToken == null) {
            return CommonResult.failed("token已经过期！");
        }
        Map<String, String> tokenMap = new HashMap<>();
        tokenMap.put("token", refreshToken);
        tokenMap.put("tokenHead", tokenHead);
        return CommonResult.success(tokenMap);
    }
}
