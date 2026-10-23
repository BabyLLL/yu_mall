package com.macro.mall.portal.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.macro.mall.common.exception.Asserts;
import com.macro.mall.mapper.UmsMemberLevelMapper;
import com.macro.mall.mapper.UmsMemberMapper;
import com.macro.mall.model.UmsMember;
import com.macro.mall.model.UmsMemberExample;
import com.macro.mall.model.UmsMemberLevel;
import com.macro.mall.model.UmsMemberLevelExample;
import com.macro.mall.portal.domain.MemberDetails;
import com.macro.mall.portal.service.UmsMemberCacheService;
import com.macro.mall.portal.service.UmsMemberService;
import com.macro.mall.portal.dao.UmsMemberWechatIdentityDao;
import com.macro.mall.portal.domain.UmsMemberWechatIdentity;
import com.macro.mall.security.util.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.CollectionUtils;

import jakarta.annotation.PostConstruct;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 会员管理Service实现类
 * Created by macro on 2018/8/3.
 */
@Service
public class UmsMemberServiceImpl implements UmsMemberService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UmsMemberServiceImpl.class);
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private UmsMemberMapper memberMapper;
    @Autowired
    private UmsMemberLevelMapper memberLevelMapper;
    @Autowired
    private UmsMemberCacheService memberCacheService;
    @Autowired
    private UmsMemberWechatIdentityDao wechatIdentityDao;
    @Autowired
    private RestTemplate restTemplate;
    @Value("${redis.key.authCode}")
    private String REDIS_KEY_PREFIX_AUTH_CODE;
    @Value("${redis.expire.authCode}")
    private Long AUTH_CODE_EXPIRE_SECONDS;
    @Value("${wechat.miniapp.app-id:}")
    private String wechatAppId;
    @Value("${wechat.miniapp.app-secret:}")
    private String wechatAppSecret;
    @Value("${wechat.miniapp.enabled:false}")
    private boolean wechatEnabled;

    @PostConstruct
    public void validateWechatConfiguration() {
        if (wechatEnabled && (StrUtil.isBlank(wechatAppId) || StrUtil.isBlank(wechatAppSecret))) {
            throw new IllegalStateException(
                    "微信小程序登录已启用，但 WECHAT_MINIAPP_APP_ID 或 WECHAT_MINIAPP_APP_SECRET 未配置");
        }
    }

    @Override
    public UmsMember getByUsername(String username) {
        UmsMember member = memberCacheService.getMember(username);
        if(member!=null) return member;
        UmsMemberExample example = new UmsMemberExample();
        example.createCriteria().andUsernameEqualTo(username);
        List<UmsMember> memberList = memberMapper.selectByExample(example);
        if (!CollectionUtils.isEmpty(memberList)) {
            member = memberList.get(0);
            memberCacheService.setMember(member);
            return member;
        }
        return null;
    }

    @Override
    public UmsMember getById(Long id) {
        return memberMapper.selectByPrimaryKey(id);
    }

    @Override
    public void register(String username, String password, String telephone, String authCode) {
        //验证验证码
        if(!verifyAuthCode(authCode,telephone)){
            Asserts.fail("验证码错误");
        }
        //查询是否已有该用户
        UmsMemberExample example = new UmsMemberExample();
        example.createCriteria().andUsernameEqualTo(username);
        example.or(example.createCriteria().andPhoneEqualTo(telephone));
        List<UmsMember> umsMembers = memberMapper.selectByExample(example);
        if (!CollectionUtils.isEmpty(umsMembers)) {
            Asserts.fail("该用户已经存在");
        }
        //没有该用户进行添加操作
        UmsMember umsMember = new UmsMember();
        umsMember.setUsername(username);
        umsMember.setPhone(telephone);
        umsMember.setPassword(passwordEncoder.encode(password));
        umsMember.setCreateTime(new Date());
        umsMember.setStatus(1);
        //获取默认会员等级并设置
        UmsMemberLevelExample levelExample = new UmsMemberLevelExample();
        levelExample.createCriteria().andDefaultStatusEqualTo(1);
        List<UmsMemberLevel> memberLevelList = memberLevelMapper.selectByExample(levelExample);
        if (!CollectionUtils.isEmpty(memberLevelList)) {
            umsMember.setMemberLevelId(memberLevelList.get(0).getId());
        }
        memberMapper.insert(umsMember);
        umsMember.setPassword(null);
    }

    @Override
    public String generateAuthCode(String telephone) {
        // 此处使用模拟生成验证码逻辑，真实环境应接入短信服务，可使用SMS4J来实现
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for(int i=0;i<6;i++){
            sb.append(random.nextInt(10));
        }
        memberCacheService.setAuthCode(telephone,sb.toString());
        return sb.toString();
    }

    @Override
    public void updatePassword(String telephone, String password, String authCode) {
        UmsMemberExample example = new UmsMemberExample();
        example.createCriteria().andPhoneEqualTo(telephone);
        List<UmsMember> memberList = memberMapper.selectByExample(example);
        if(CollectionUtils.isEmpty(memberList)){
            Asserts.fail("该账号不存在");
        }
        //验证验证码
        if(!verifyAuthCode(authCode,telephone)){
            Asserts.fail("验证码错误");
        }
        UmsMember umsMember = memberList.get(0);
        umsMember.setPassword(passwordEncoder.encode(password));
        memberMapper.updateByPrimaryKeySelective(umsMember);
        memberCacheService.delMember(umsMember.getId());
    }

    @Override
    public UmsMember getCurrentMember() {
        SecurityContext ctx = SecurityContextHolder.getContext();
        Authentication auth = ctx.getAuthentication();
        MemberDetails memberDetails = (MemberDetails) auth.getPrincipal();
        return memberDetails.getUmsMember();
    }

    @Override
    public void updateIntegration(Long id, Integer integration) {
        UmsMember record=new UmsMember();
        record.setId(id);
        record.setIntegration(integration);
        memberMapper.updateByPrimaryKeySelective(record);
        memberCacheService.delMember(id);
    }

    @Override
    public UmsMember updateInfo(Long memberId, String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            Asserts.fail("昵称不能为空");
        }
        String trimmed = nickname.trim();
        if (trimmed.length() > 64) {
            Asserts.fail("昵称长度不能超过64个字符");
        }
        UmsMember record = new UmsMember();
        record.setId(memberId);
        record.setNickname(trimmed);
        memberMapper.updateByPrimaryKeySelective(record);
        memberCacheService.delMember(memberId);
        return memberMapper.selectByPrimaryKey(memberId);
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        UmsMember member = getByUsername(username);
        if(member!=null){
            return new MemberDetails(member);
        }
        throw new UsernameNotFoundException("用户名或密码错误");
    }

    @Override
    public String login(String username, String password) {
        String token = null;
        //密码需要客户端加密后传递
        try {
            UserDetails userDetails = loadUserByUsername(username);
            if(!passwordEncoder.matches(password,userDetails.getPassword())){
                throw new BadCredentialsException("密码不正确");
            }
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            token = jwtTokenUtil.generateToken(userDetails);
        } catch (AuthenticationException e) {
            LOGGER.warn("登录异常:{}", e.getMessage());
        }
        return token;
    }

    @Override
    public String refreshToken(String token) {
        return jwtTokenUtil.refreshHeadToken(token);
    }

    @Override
    @SuppressWarnings("unchecked")
    @Transactional
    public UserDetails loginByWechat(String code, String clientId, String nickname, String avatarUrl) {
        if (!wechatEnabled || StrUtil.isBlank(wechatAppId) || StrUtil.isBlank(wechatAppSecret)) {
            Asserts.fail("微信登录尚未配置");
        }
        if (StrUtil.isBlank(code) || StrUtil.isBlank(clientId)) {
            Asserts.fail("微信登录参数不完整");
        }
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid="
                + URLEncoder.encode(wechatAppId, StandardCharsets.UTF_8)
                + "&secret=" + URLEncoder.encode(wechatAppSecret, StandardCharsets.UTF_8)
                + "&js_code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                + "&grant_type=authorization_code";
        Map<String, Object> result;
        try {
//            result = restTemplate.getForObject(url, Map.class);
            // 先获取返回字符串，不要直接转Map
            String respStr = restTemplate.getForObject(url, String.class);
            // Hutool JSONUtil 转map
            // 新增日志，看微信到底返回了啥
            LOGGER.info("微信jscode2session原始返回：{}", respStr);
            result = JSONUtil.parseObj(respStr);
        } catch (Exception e) {
            LOGGER.warn("微信登录凭证交换失败: {}", e.getMessage());
            Asserts.fail("微信登录服务暂不可用");
            return null;
        }
        if (result == null || result.get("openid") == null || result.get("errcode") != null) {
            Asserts.fail("微信授权凭证无效");
        }
        String openId = String.valueOf(result.get("openid"));
        String unionId = result.get("unionid") == null ? null : String.valueOf(result.get("unionid"));
        UmsMemberWechatIdentity identity = wechatIdentityDao.findByAppIdAndOpenId(wechatAppId, openId);
        UmsMember member;
        if (identity == null) {
            member = createWechatMember(openId, nickname, avatarUrl);
            identity = new UmsMemberWechatIdentity();
            identity.setAppId(wechatAppId);
            identity.setOpenId(openId);
            identity.setUnionId(unionId);
            identity.setMemberId(member.getId());
            identity.setCreateTime(new Date());
            identity.setUpdateTime(new Date());
            wechatIdentityDao.insert(identity);
        } else {
            member = getById(identity.getMemberId());
            if (member == null || !Integer.valueOf(1).equals(member.getStatus())) {
                Asserts.fail("该会员已被禁用");
            }
            updateWechatProfile(member, nickname, avatarUrl);
        }
        return new MemberDetails(member);
    }

    private UmsMember createWechatMember(String openId, String nickname, String avatarUrl) {
        UmsMember member = new UmsMember();
        member.setUsername("wx_" + digest(openId).substring(0, 32));
        member.setNickname(StrUtil.isBlank(nickname) ? "微信用户" : nickname);
        member.setIcon(avatarUrl);
        member.setStatus(1);
        member.setSourceType(2);
        member.setCreateTime(new Date());
        UmsMemberLevelExample levelExample = new UmsMemberLevelExample();
        levelExample.createCriteria().andDefaultStatusEqualTo(1);
        List<UmsMemberLevel> levels = memberLevelMapper.selectByExample(levelExample);
        if (!CollectionUtils.isEmpty(levels)) {
            member.setMemberLevelId(levels.get(0).getId());
        }
        memberMapper.insertSelective(member);
        return member;
    }

    private void updateWechatProfile(UmsMember member, String nickname, String avatarUrl) {
        boolean changed = false;
        UmsMember update = new UmsMember();
        update.setId(member.getId());
        if (StrUtil.isNotBlank(nickname) && !nickname.equals(member.getNickname())) {
            update.setNickname(nickname);
            changed = true;
        }
        if (StrUtil.isNotBlank(avatarUrl) && !avatarUrl.equals(member.getIcon())) {
            update.setIcon(avatarUrl);
            changed = true;
        }
        if (changed) {
            memberMapper.updateByPrimaryKeySelective(update);
            memberCacheService.delMember(member.getId());
            if (update.getNickname() != null) member.setNickname(update.getNickname());
            if (update.getIcon() != null) member.setIcon(update.getIcon());
        }
    }

    private String digest(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : bytes) builder.append(String.format("%02x", item));
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    //对输入的验证码进行校验
    private boolean verifyAuthCode(String authCode, String telephone){
        if(StrUtil.isEmpty(authCode)){
            return false;
        }
        String realAuthCode = memberCacheService.getAuthCode(telephone);
        return authCode.equals(realAuthCode);
    }

}
