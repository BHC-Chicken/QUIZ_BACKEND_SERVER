package com.example.quiz.dto.room.response;

import com.example.quiz.enums.Role;
import com.example.quiz.vo.InGameUser;

import java.util.Set;

public record RoomEnterResponse (
        Long roomId,
        String roomName,
        Long topicId,
        Integer maxPeople,
        Integer quizCount,
        boolean removeStatus,
        Role role,
        InGameUser inGameUser,
        Set<InGameUser> participants
) {}
