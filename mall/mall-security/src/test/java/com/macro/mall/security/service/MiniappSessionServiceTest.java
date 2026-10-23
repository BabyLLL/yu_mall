package com.macro.mall.security.service;

import com.macro.mall.common.service.RedisService;
import com.macro.mall.security.model.MiniappSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MiniappSessionServiceTest {
    private MiniappSessionService service;
    private MemoryRedisService redis;

    @BeforeEach
    void setUp() {
        service = new MiniappSessionService();
        redis = new MemoryRedisService();
        ReflectionTestUtils.setField(service, "redisService", redis);
        ReflectionTestUtils.setField(service, "keyPrefix", "test:miniapp");
        ReflectionTestUtils.setField(service, "sessionTtl", 60L);
    }

    @Test
    void activatingAnotherIdentityRequiresLogout() {
        MiniappSession member = service.activateIfAbsent("client-1", MiniappSessionService.MEMBER, "member-1");
        assertTrue(service.isActive("client-1", member.getSessionId(), "member", "member-1"));

        MiniappSession admin = service.activateIfAbsent("client-1", MiniappSessionService.ADMIN, "admin-1");

        assertNull(admin);
        assertTrue(service.isActive("client-1", member.getSessionId(), "member", "member-1"));
        assertEquals(60L, redis.lastTtl);
    }

    @Test
    void matchingLogoutDeletesSessionAndAllowsAnotherLogin() {
        MiniappSession session = service.activateIfAbsent("client-1", MiniappSessionService.MEMBER, "member-1");

        assertFalse(service.invalidateIfActive(
                "client-1", "wrong-session", MiniappSessionService.MEMBER, "member-1"));
        assertTrue(service.isActive("client-1", session.getSessionId(), "member", "member-1"));

        assertTrue(service.invalidateIfActive(
                "client-1", session.getSessionId(), MiniappSessionService.MEMBER, "member-1"));
        assertFalse(service.isActive("client-1", session.getSessionId(), "member", "member-1"));

        assertNotNull(service.activateIfAbsent(
                "client-1", MiniappSessionService.ADMIN, "admin:admin-1"));
    }

    @Test
    void concurrentActivationsHaveOneWinner() throws Exception {
        int attempts = 12;
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        for (int index = 0; index < attempts; index++) {
            int subject = index;
            executor.submit(() -> {
                ready.countDown();
                start.await();
                MiniappSession session = service.activateIfAbsent(
                        "client-1", MiniappSessionService.MEMBER, "member-" + subject);
                if (session != null) {
                    successes.incrementAndGet();
                }
                return null;
            });
        }
        assertTrue(ready.await(2, TimeUnit.SECONDS));
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS));
        assertEquals(1, successes.get());
    }

    private static class MemoryRedisService implements RedisService {
        private final Map<String, Object> values = new ConcurrentHashMap<>();
        private long lastTtl;
        public void set(String key, Object value, long time) { lastTtl = time; values.put(key, value); }
        public void set(String key, Object value) { values.put(key, value); }
        public Boolean setIfAbsent(String key, Object value, long time) {
            lastTtl = time;
            return values.putIfAbsent(key, value) == null;
        }
        public Object get(String key) { return values.get(key); }
        public Boolean del(String key) { return values.remove(key) != null; }
        public Boolean compareAndDelete(String key, Object expectedValue) {
            return values.remove(key, expectedValue);
        }
        public Long del(java.util.List<String> keys) { keys.forEach(values::remove); return (long) keys.size(); }
        public Boolean expire(String key, long time) { return values.containsKey(key); }
        public Long getExpire(String key) { return -1L; }
        public Boolean hasKey(String key) { return values.containsKey(key); }
        public Long incr(String key, long delta) { throw new UnsupportedOperationException(); }
        public Long decr(String key, long delta) { throw new UnsupportedOperationException(); }
        public Object hGet(String key, String hashKey) { throw new UnsupportedOperationException(); }
        public Boolean hSet(String key, String hashKey, Object value, long time) { throw new UnsupportedOperationException(); }
        public void hSet(String key, String hashKey, Object value) { throw new UnsupportedOperationException(); }
        public Map<Object, Object> hGetAll(String key) { throw new UnsupportedOperationException(); }
        public Boolean hSetAll(String key, Map<String, Object> map, long time) { throw new UnsupportedOperationException(); }
        public void hSetAll(String key, Map<String, ?> map) { throw new UnsupportedOperationException(); }
        public void hDel(String key, Object... hashKey) { throw new UnsupportedOperationException(); }
        public Boolean hHasKey(String key, String hashKey) { throw new UnsupportedOperationException(); }
        public Long hIncr(String key, String hashKey, Long delta) { throw new UnsupportedOperationException(); }
        public Long hDecr(String key, String hashKey, Long delta) { throw new UnsupportedOperationException(); }
        public java.util.Set<Object> sMembers(String key) { throw new UnsupportedOperationException(); }
        public Long sAdd(String key, Object... values) { throw new UnsupportedOperationException(); }
        public Long sAdd(String key, long time, Object... values) { throw new UnsupportedOperationException(); }
        public Boolean sIsMember(String key, Object value) { throw new UnsupportedOperationException(); }
        public Long sSize(String key) { throw new UnsupportedOperationException(); }
        public Long sRemove(String key, Object... values) { throw new UnsupportedOperationException(); }
        public java.util.List<Object> lRange(String key, long start, long end) { throw new UnsupportedOperationException(); }
        public Long lSize(String key) { throw new UnsupportedOperationException(); }
        public Object lIndex(String key, long index) { throw new UnsupportedOperationException(); }
        public Long lPush(String key, Object value) { throw new UnsupportedOperationException(); }
        public Long lPush(String key, Object value, long time) { throw new UnsupportedOperationException(); }
        public Long lPushAll(String key, Object... values) { throw new UnsupportedOperationException(); }
        public Long lPushAll(String key, Long time, Object... values) { throw new UnsupportedOperationException(); }
        public Long lRemove(String key, long count, Object value) { throw new UnsupportedOperationException(); }
    }
}
