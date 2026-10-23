package com.macro.mall.portal.component;

import com.macro.mall.common.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 取货码生成器：Redis 按天自增，格式 N001、N002……每天从 N001 重新开始。
 * key 形如 stall:pickup:code:20261012，TTL 48h 兜底清理。
 * Created by macro on 2026/10/12.
 */
@Component
public class PickupCodeGenerator {

    private static final String KEY_PREFIX = "stall:pickup:code:";
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final long KEY_TTL_SECONDS = 48 * 60 * 60;

    @Autowired
    private RedisService redisService;

    @Autowired
    private Clock clock;

    /**
     * 生成当日下一个取货码
     */
    public String next() {
        String day = LocalDate.now(clock).format(DAY_FORMAT);
        String key = KEY_PREFIX + day;
        Long seq = redisService.incr(key, 1);
        // 首次递增时设置过期，重复设置无害
        redisService.expire(key, KEY_TTL_SECONDS);
        return String.format("N%03d", seq);
    }
}
