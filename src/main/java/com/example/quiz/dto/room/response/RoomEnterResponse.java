package com.example.quiz.dto.room.response;

import com.example.quiz.vo.InGameUser;
import lombok.ToString;
import java.util.Set;

public record RoomEnterResponse (
        Long roomId,
        String roomName,
        Long topicId,
        Integer maxPeople,
        Integer quizCount,
        boolean removeStatus,
        boolean isAdmin,
        boolean isInGame,
        Set<InGameUser> participants
) {}
