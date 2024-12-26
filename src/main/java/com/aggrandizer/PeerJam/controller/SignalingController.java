// src/main/java/com/example/webrtcsignaling/controller/SignalingController.java
package com.aggrandizer.PeerJam.controller;

import com.aggrandizer.PeerJam.model.SignalingMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class SignalingController {

    @MessageMapping("/signal")
    @SendTo("/topic/signaling")
    public SignalingMessage relaySignaling(SignalingMessage message) {
        return message;
    }
}