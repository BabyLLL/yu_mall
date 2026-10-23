package com.macro.mall.security.model;

import java.io.Serializable;

/**
 * The single active authentication session for one miniapp installation.
 */
public class MiniappSession implements Serializable {
    private String clientId;
    private String sessionId;
    private String authType;
    private String subject;

    public MiniappSession() {
    }

    public MiniappSession(String clientId, String sessionId, String authType, String subject) {
        this.clientId = clientId;
        this.sessionId = sessionId;
        this.authType = authType;
        this.subject = subject;
    }

    public String getClientId() {
        return clientId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getAuthType() {
        return authType;
    }

    public String getSubject() {
        return subject;
    }
}
