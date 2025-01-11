package com.example.quiz.controller;

import com.example.quiz.config.auth.annotation.user.LoginUser;
import com.example.quiz.dto.User.LoginUserRequest;
import com.example.quiz.dto.room.request.RoomCreateRequest;
import com.example.quiz.dto.room.request.RoomModifyRequest;
import com.example.quiz.dto.room.response.RoomEnterResponse;
import com.example.quiz.dto.room.response.RoomListResponse;
import com.example.quiz.dto.room.response.RoomModifyResponse;
import com.example.quiz.dto.room.response.RoomResponse;
import com.example.quiz.service.RoomProducerService;
import com.example.quiz.service.RoomService;
import com.example.quiz.vo.InGameUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class RoomController {
    private final RoomService roomService;
    private final RoomProducerService roomProducerService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @PostMapping(value = "/room")
    public String createRoom(RoomCreateRequest roomRequest, @LoginUser LoginUserRequest loginUserRequest) throws IllegalAccessException {
        RoomResponse roomResponse = roomProducerService.createRoom(roomRequest, loginUserRequest);

        return "redirect:/room/" + roomResponse.roomId();
    }

    @GetMapping("/room-list")
    public ModelAndView getRoomList(@RequestParam(name = "page") Optional<Integer> page) {
        int index = page.orElse(1) - 1;
        Page<RoomListResponse> roomListResponses = roomProducerService.roomList(index);
        Map<String, Object> map = new HashMap<>();

        String roomIds = roomListResponses.stream().map(RoomListResponse::roomId).map(String::valueOf).collect(
                Collectors.joining(","));
        map.put("roomList", roomListResponses);
        map.put("roomIds", roomIds);

        return new ModelAndView("index", map);
    }

    @GetMapping("/room/{roomId}")
    public ModelAndView enterRoom(@PathVariable Long roomId, @LoginUser LoginUserRequest loginUserRequest) throws IllegalAccessException {
        RoomEnterResponse roomEnterResponse = roomService.enterRoom(roomId, loginUserRequest);
        Map<String, Object> map = new HashMap<>();
        map.put("roomInfo", roomEnterResponse);
        // 참가자 입장 메시지 브로드캐스트
        simpMessagingTemplate.convertAndSend("/pub/room/" + roomId + "/participants", roomEnterResponse.participants());

        Set<InGameUser> set = roomEnterResponse.participants();
        for(InGameUser participant : set) {
            if(Objects.equals(participant.getId(), loginUserRequest.userId())) {
                // 실시간 참가자 정보 브로드캐스트
                simpMessagingTemplate.convertAndSend("/pub/room/" + roomId + "/participants", participant);
            }
        }
        return new ModelAndView("room", map);
    }

    @ResponseBody
    @PatchMapping("/room/{roomId}")
    public ResponseEntity<RoomModifyResponse> modifyRoom(@PathVariable Long roomId, RoomModifyRequest request) throws IllegalAccessException {
        RoomModifyResponse roomModifyResponse = roomService.modifyRoom(request, roomId);

        return ResponseEntity.ok(roomModifyResponse);
    }
}