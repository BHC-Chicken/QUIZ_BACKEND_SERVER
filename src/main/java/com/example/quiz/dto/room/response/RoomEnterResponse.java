package com.example.quiz.dto.room.response;

public record RoomEnterResponse (Long roomId, String roomName, Long topicId, Integer maxPeople, Integer quizCount, boolean removeStatus){
}
