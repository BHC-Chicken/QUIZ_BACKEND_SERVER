package com.example.quiz.controller;

import com.example.quiz.dto.room.request.RoomCreateRequest;
import com.example.quiz.dto.room.request.RoomModifyRequest;
import com.example.quiz.dto.room.response.RoomEnterResponse;
import com.example.quiz.dto.room.response.RoomListResponse;
import com.example.quiz.dto.room.response.RoomModifyResponse;
import com.example.quiz.dto.room.response.RoomResponse;
import com.example.quiz.service.RoomProducerService;
import com.example.quiz.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class RoomController {
    private final RoomService roomService;
    private final RoomProducerService roomProducerService;

    @PostMapping(value = "/room")
    public String createRoom(RoomCreateRequest roomRequest) {
        RoomResponse roomResponse = roomProducerService.createRoom(roomRequest);

        return "redirect:/room/" + roomResponse.roomId();

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
    public ModelAndView enterRoom(@PathVariable Long roomId) throws IllegalAccessException {
        RoomEnterResponse roomEnterResponse = roomService.enterRoom(roomId);
        Map<String, Object> map = new HashMap<>();
        map.put("roomInfo", roomEnterResponse);

        return new ModelAndView("room", map);
    }

    @ResponseBody
    @PatchMapping("/room/{roomId}")
    public ResponseEntity<RoomModifyResponse> modifyRoom(@PathVariable Long roomId, RoomModifyRequest request) {
        RoomModifyResponse roomModifyResponse = roomService.modifyRoom(request, roomId);

        return ResponseEntity.ok(roomModifyResponse);
    }
}