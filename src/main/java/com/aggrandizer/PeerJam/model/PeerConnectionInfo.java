package com.aggrandizer.PeerJam.model;

import org.springframework.web.socket.WebSocketSession;

public class PeerConnectionInfo {
    private String userId;
    private WebSocketSession session;

    public PeerConnectionInfo(String userId, WebSocketSession session) {
        this.userId = userId;
        this.session = session;
    }

    public String getUserId() {
        return userId;
    }

    public WebSocketSession getSession() {
        return session;
    }
}