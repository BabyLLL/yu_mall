package com.macro.mall.security.service;

import com.macro.mall.common.service.RedisService;
import com.macro.mall.security.model.MiniappSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Stores the only active identity session for one miniapp installation.
 */
@Service
public class MiniappSessionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MiniappSessionService.class);
    public static final String MEMBER = "member";
    public static final String ADMIN = "admin";

    @Autowired
    private RedisService redisService;

    @Value("${miniapp.auth.session-key-prefix:mall:miniapp:session}")
    private String keyPrefix;

    @Value("${miniapp.auth.session-ttl:604800}")
    private long sessionTtl;

    public MiniappSession activateIfAbsent(String clientId, String authType, String subject) {
        if (isBlank(clientId) || isBlank(authType) || isBlank(subject)) {
            throw new IllegalArgumentException("miniapp session metadata must not be blank");
        }
        MiniappSession session = new MiniappSession(clientId, UUID.randomUUID().toString(), authType, subject);
        boolean created = Boolean.TRUE.equals(
                redisService.setIfAbsent(key(clientId), serialize(session), sessionTtl));
        if (!created) {
            LOGGER.info("miniapp login rejected because a session is active: clientSuffix={}, requestedType={}",
                    suffix(clientId), authType);
            return null;
        }
        LOGGER.info("miniapp active session created: clientSuffix={}, authType={}", suffix(clientId), authType);
        return session;
    }

    public boolean hasActiveSession(String clientId) {
        return !isBlank(clientId) && Boolean.TRUE.equals(redisService.hasKey(key(clientId)));
    }

    public boolean invalidateIfActive(String clientId, String sessionId, String authType, String subject) {
        if (isBlank(clientId) || isBlank(sessionId) || isBlank(authType) || isBlank(subject)) {
            return false;
        }
        String redisKey = key(clientId);
        Object current = redisService.get(redisKey);
        if (!matchesStoredValue(current, clientId, sessionId, authType, subject)) {
            LOGGER.info("miniapp logout rejected because the session did not match: clientSuffix={}, authType={}",
                    suffix(clientId), authType);
            return false;
        }
        boolean deleted = current instanceof String
                ? Boolean.TRUE.equals(redisService.compareAndDelete(redisKey, current))
                : Boolean.TRUE.equals(redisService.del(redisKey));
        LOGGER.info("miniapp logout result: clientSuffix={}, authType={}, deleted={}",
                suffix(clientId), authType, deleted);
        return deleted;
    }

    public boolean isActive(String clientId, String sessionId, String authType, String subject) {
        if (clientId == null || sessionId == null || authType == null || subject == null) {
            return false;
        }
        Object value = redisService.get(key(clientId));
        return matchesStoredValue(value, clientId, sessionId, authType, subject);
    }

    private boolean matchesStoredValue(Object value, String clientId, String sessionId,
                                       String authType, String subject) {
        if (value instanceof String serialized) {
            MiniappSession session = deserialize(serialized);
            return session != null && matches(session.getClientId(), session.getSessionId(),
                    session.getAuthType(), session.getSubject(), clientId, sessionId, authType, subject);
        }
        if (value instanceof MiniappSession session) {
            return matches(session.getClientId(), session.getSessionId(), session.getAuthType(), session.getSubject(),
                    clientId, sessionId, authType, subject);
        }
        if (value instanceof Map<?, ?> map) {
            return matches(stringValue(map.get("clientId")), stringValue(map.get("sessionId")),
                    stringValue(map.get("authType")), stringValue(map.get("subject")),
                    clientId, sessionId, authType, subject);
        }
        return false;
    }

    private String serialize(MiniappSession session) {
        return encode(session.getClientId()) + "." + encode(session.getSessionId()) + "."
                + encode(session.getAuthType()) + "." + encode(session.getSubject());
    }

    private MiniappSession deserialize(String value) {
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) {
            return null;
        }
        try {
            return new MiniappSession(decode(parts[0]), decode(parts[1]), decode(parts[2]), decode(parts[3]));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private boolean matches(String storedClientId, String storedSessionId, String storedAuthType, String storedSubject,
                            String clientId, String sessionId, String authType, String subject) {
        return clientId.equals(storedClientId)
                && sessionId.equals(storedSessionId)
                && authType.equals(storedAuthType)
                && subject.equals(storedSubject);
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String key(String clientId) {
        return keyPrefix + ":" + clientId;
    }

    private String suffix(String clientId) {
        return clientId.length() <= 6 ? "***" : clientId.substring(clientId.length() - 6);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
