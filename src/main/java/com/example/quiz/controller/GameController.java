package com.example.quiz.controller;

import com.example.quiz.dto.request.RequestUserId;
import com.example.quiz.dto.response.ResponseMessage;
import com.example.quiz.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
public class GameController {
    private final SimpMessagingTemplate messagingTemplate;
    private final GameService gameService;

    @MessageMapping("/{id}/ready")
    public void ready(@DestinationVariable String id, RequestUserId requestUserId) {
        ResponseMessage responseMessage = gameService.toggleReadyStatus(id, requestUserId.userId());

        messagingTemplate.convertAndSend("/pub/room/"+id, responseMessage);
    }

    @MessageMapping("/{id}/start")
    public void start(@DestinationVariable String id) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("gameStarted", true);

        messagingTemplate.convertAndSend("/pub/room/"+id, msg);
    }
}
