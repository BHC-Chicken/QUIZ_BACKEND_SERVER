package com.example.quiz.controller;

import com.example.quiz.dto.request.RequestUserId;
import com.example.quiz.dto.response.ResponseMessage;
import com.example.quiz.dto.request.RequestUserInfoAnswer;
import com.example.quiz.dto.request.RequestAnswer;
import com.example.quiz.dto.response.ResponseQuiz;
import com.example.quiz.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class GameController {

     // 메세지 템플릿을 사용하여 데이터를 전달
     // 이유는 게임의 상태를 정의 할때와 게임의 시작할때 전송하는 데이터의 구조가 변경이 된므로 템플릿사용
    private final SimpMessagingTemplate messagingTemplate;
    private final GameService gameService;

    @MessageMapping("/{id}/ready")
    public void ready(@DestinationVariable String id, RequestUserId requestUserId) {
        log.info("user ID is {}", requestUserId.userId());
        ResponseMessage responseMessage = gameService.toggleReadyStatus(id, requestUserId.userId());
        log.info("role : {}, userId : {}, readyStatus : {}, email : {}" , responseMessage.role(), responseMessage.userId(), responseMessage.readyStatus(), responseMessage.email());
        messagingTemplate.convertAndSend("/pub/room/"+id,responseMessage);
    }

    @MessageMapping("/{id}/send")
    public void sendQuize(@DestinationVariable String id, RequestUserInfoAnswer userInfoAnswer){
        log.info("응답이 들어왔습니다");
        ResponseQuiz responseQuiz = gameService.sendQuiz(id,userInfoAnswer);

        messagingTemplate.convertAndSend("/pub/"+id+"/send", responseQuiz);
    }

    @MessageMapping("/{id}/check")
    public void checkQuize(@DestinationVariable String id, RequestAnswer requestAnswer){
        log.info("응답");
        ResponseQuiz responseQuiz = gameService.checkAnswer(id, requestAnswer);

        messagingTemplate.convertAndSend("/pub/"+id+"/check",responseQuiz);
    }
}
