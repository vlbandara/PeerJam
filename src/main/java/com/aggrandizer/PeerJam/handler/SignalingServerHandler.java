package com.aggrandizer.PeerJam.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class SignalingServerHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = session.getId();
        sessions.put(userId, session);
        broadcastMessage(userId, "joined");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        Map<String, Object> data = objectMapper.readValue(payload, Map.class);
        String type = (String) data.get("type");

        switch (type) {
            case "offer":
                handleOffer(session, data);
                break;
            case "answer":
                handleAnswer(session, data);
                break;
            case "ice-candidate":
                handleIceCandidate(session, data);
                break;
            default:
                break;
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = session.getId();
        sessions.remove(userId);
        broadcastMessage(userId, "left");
    }

    private void handleOffer(WebSocketSession session, Map<String, Object> data) throws IOException {
        String to = (String) data.get("to");
        String from = session.getId();
        data.put("from", from);
        sendMessage(to, objectMapper.writeValueAsString(data));
    }

    private void handleAnswer(WebSocketSession session, Map<String, Object> data) throws IOException {
        String to = (String) data.get("to");
        data.remove("to");
        sendMessage(to, objectMapper.writeValueAsString(data));
    }

    private void handleIceCandidate(WebSocketSession session, Map<String, Object> data) throws IOException {
        String to = (String) data.get("to");
        data.remove("to");
        sendMessage(to, objectMapper.writeValueAsString(data));
    }

    private void sendMessage(String userId, String message) throws IOException {
        WebSocketSession session = sessions.get(userId);
        if (session != null) {
            session.sendMessage(new TextMessage(message));
        }
    }

    private void broadcastMessage(String userId, String status) throws IOException {
        for (WebSocketSession session : sessions.values()) {
            if (!session.getId().equals(userId)) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("type", status, "userId", userId))));
            }
        }
    }
}