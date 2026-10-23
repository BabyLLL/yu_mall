package com.macro.mall.portal.component;

import com.macro.mall.common.service.RedisService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PickupCodeGeneratorTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Mock
    private RedisService redisService;

    private PickupCodeGenerator newGenerator(Instant now) {
        Clock fixedClock = Clock.fixed(now, ZONE);
        PickupCodeGenerator g = new PickupCodeGenerator();
        try {
            java.lang.reflect.Field rf = PickupCodeGenerator.class.getDeclaredField("redisService");
            rf.setAccessible(true);
            rf.set(g, redisService);
            java.lang.reflect.Field cf = PickupCodeGenerator.class.getDeclaredField("clock");
            cf.setAccessible(true);
            cf.set(g, fixedClock);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return g;
    }

    private String keyOf(String yyyyMMdd) {
        return "stall:pickup:code:" + yyyyMMdd;
    }

    private String day(Instant now) {
        return LocalDate.ofInstant(now, ZONE).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    @Test
    void firstCodeOfDayIsN001() {
        Instant now = Instant.parse("2026-10-12T04:00:00Z"); // 上海时间 12:00
        PickupCodeGenerator generator = newGenerator(now);
        when(redisService.incr(eq(keyOf(day(now))), eq(1L))).thenReturn(1L);
        assertEquals("N001", generator.next());
        verify(redisService).expire(eq(keyOf(day(now))), anyLong());
    }

    @Test
    void sameDayCodesIncrement() {
        Instant now = Instant.parse("2026-10-12T04:00:00Z");
        PickupCodeGenerator generator = newGenerator(now);
        when(redisService.incr(eq(keyOf(day(now))), eq(1L))).thenReturn(1L, 2L, 3L);
        assertEquals("N001", generator.next());
        assertEquals("N002", generator.next());
        assertEquals("N003", generator.next());
    }

    @Test
    void crossDaySequenceResets() {
        Instant yesterday = Instant.parse("2026-10-11T15:00:00Z"); // 上海时间 10-11 23:00
        Instant today = Instant.parse("2026-10-11T16:30:00Z");    // 上海时间 10-12 00:30
        PickupCodeGenerator generator = newGenerator(yesterday);
        // 昨日第 42 张
        when(redisService.incr(eq(keyOf(day(yesterday))), eq(1L))).thenReturn(42L);
        assertEquals("N042", generator.next());
        // 跨天后第一张：不同 key，重置为 N001
        PickupCodeGenerator nextDay = newGenerator(today);
        when(redisService.incr(eq(keyOf(day(today))), eq(1L))).thenReturn(1L);
        assertEquals("N001", nextDay.next());
    }

    @Test
    void largeSequencePadsToFourDigits() {
        Instant now = Instant.parse("2026-10-12T04:00:00Z");
        PickupCodeGenerator generator = newGenerator(now);
        when(redisService.incr(anyString(), anyLong())).thenReturn(1000L);
        assertEquals("N1000", generator.next());
    }
}
