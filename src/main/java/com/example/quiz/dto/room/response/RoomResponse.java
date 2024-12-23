package com.example.quiz.dto.room.response;

public record RoomResponse(Long roomId, String roomName, Long topicId, Integer maxPeople,
                           Integer quizCnt, Integer currentPeople) {
}
