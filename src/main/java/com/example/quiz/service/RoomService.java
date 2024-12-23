package com.example.quiz.service;

import com.example.quiz.dto.room.request.RoomModifyRequest;
import com.example.quiz.dto.room.response.RoomEnterResponse;
import com.example.quiz.dto.room.response.RoomModifyResponse;
import com.example.quiz.entity.Game;
import com.example.quiz.entity.Room;
import com.example.quiz.entity.User;
import com.example.quiz.enums.Role;
import com.example.quiz.repository.GameRepository;
import com.example.quiz.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomService {
    private final RoomRepository roomRepository;
    private final GameRepository gameRepository;

    public RoomEnterResponse enterRoom(long roomId) throws IllegalAccessException {
        Room room = roomRepository.findById(roomId).orElseThrow(IllegalArgumentException::new);
        User user = new User(5L, "user", "sample@sampe.co", Role.USER, false);

        if (user == null) {
            throw new IllegalAccessException();
        }

        Game game = gameRepository.findById(String.valueOf(roomId)).orElseThrow();

        if (room.getMaxPeople() <= game.getGameUser().size()) {
            throw new IllegalArgumentException();
        }

        game.getGameUser().add(user);
        gameRepository.save(game);

        return new RoomEnterResponse(room.getRoomId(), room.getRoomName(), room.getTopicId(), room.getMaxPeople(), room.getQuizCount(), room.getRemoveStatus());
    }

    @Transactional
    public RoomModifyResponse modifyRoom(RoomModifyRequest request, long roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow(IllegalArgumentException::new);

        if (request.roomName() != null) {
            room.changeRoomName(request.roomName());
        }

        if (request.topicId() != null) {
            room.changeSubject(request.topicId());
        }

        return new RoomModifyResponse(room.getRoomName(), room.getTopicId());
    }
}