package com.example.quiz.dto.room.response;

public record RoomListResponse (Long roomId, String roomName, Long topicId, Integer maxPeople, Integer quizCount, Integer currentPeople) {

}
