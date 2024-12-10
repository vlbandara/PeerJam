package com.aggrandizer.PeerJam.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SignalingServerHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = session.getId();
        sessions.put(userId, session);

        // Send current peer list to the newly connected user
        sendPeerList(session);

        // Notify all other users about the new peer
        broadcastPeerList();
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
            case "peer-list":
                sendPeerList(session);
                break;
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = session.getId();
        sessions.remove(userId);

        // Broadcast updated peer list
        broadcastPeerList();
    }

    private void handleOffer(WebSocketSession session, Map<String, Object> data) throws IOException {
        String to = (String) data.get("to");
        String from = session.getId();

        Map<String, Object> offerMessage = new HashMap<>();
        offerMessage.put("type", "offer");
        offerMessage.put("from", from);
        offerMessage.put("payload", data.get("payload"));

        sendMessage(to, objectMapper.writeValueAsString(offerMessage));
    }

    private void handleAnswer(WebSocketSession session, Map<String, Object> data) throws IOException {
        String to = (String) data.get("to");
        String from = session.getId();

        Map<String, Object> answerMessage = new HashMap<>();
        answerMessage.put("type", "answer");
        answerMessage.put("from", from);
        answerMessage.put("payload", data.get("payload"));

        sendMessage(to, objectMapper.writeValueAsString(answerMessage));
    }

    private void handleIceCandidate(WebSocketSession session, Map<String, Object> data) throws IOException {
        String to = (String) data.get("to");
        String from = session.getId();

        Map<String, Object> candidateMessage = new HashMap<>();
        candidateMessage.put("type", "ice-candidate");
        candidateMessage.put("from", from);
        candidateMessage.put("payload", data.get("payload"));

        sendMessage(to, objectMapper.writeValueAsString(candidateMessage));
    }

    private void sendMessage(String userId, String message) throws IOException {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(message));
        }
    }

    private void sendPeerList(WebSocketSession session) throws IOException {
        List<String> peerIds = new ArrayList<>(sessions.keySet());
        peerIds.remove(session.getId());  // Remove the current user's ID

        Map<String, Object> peerListMessage = new HashMap<>();
        peerListMessage.put("type", "peer-list");
        peerListMessage.put("payload", peerIds);

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(peerListMessage)));
    }

    private void broadcastPeerList() throws IOException {
        List<String> peerIds = new ArrayList<>(sessions.keySet());

        for (WebSocketSession session : sessions.values()) {
            List<String> otherPeerIds = new ArrayList<>(peerIds);
            otherPeerIds.remove(session.getId());  // Remove the current user's ID

            Map<String, Object> peerListMessage = new HashMap<>();
            peerListMessage.put("type", "peer-list");
            peerListMessage.put("payload", otherPeerIds);

            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(peerListMessage)));
        }
    }
}