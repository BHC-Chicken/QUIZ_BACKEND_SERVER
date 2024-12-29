package com.example.quiz.service;

import com.example.quiz.dto.User.LoginUserRequest;
import com.example.quiz.dto.room.request.RoomModifyRequest;
import com.example.quiz.dto.room.response.RoomEnterResponse;
import com.example.quiz.dto.room.response.RoomModifyResponse;
import com.example.quiz.entity.Game;
import com.example.quiz.entity.Room;
import com.example.quiz.entity.User;
import com.example.quiz.mapper.RoomMapper;
import com.example.quiz.repository.GameRepository;
import com.example.quiz.repository.RoomRepository;
import com.example.quiz.repository.UserRepository;
import com.example.quiz.vo.InGameUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomService {
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final GameRepository gameRepository;


    public RoomEnterResponse enterRoom(long roomId, LoginUserRequest loginUserRequest) throws IllegalAccessException {
        Room room = roomRepository.findById(roomId).orElseThrow(IllegalArgumentException::new);

        if (loginUserRequest == null) {
            throw new IllegalAccessException();
        }

        Game game = gameRepository.findById(String.valueOf(roomId)).orElseThrow();
        InGameUser inGameUser = findUser(roomId, loginUserRequest);

        game.getGameUser().add(inGameUser);
        gameRepository.save(game);

        return RoomMapper.INSTANCE.RoomToRoomEnterResponse(room);
    }

    @Transactional
    public RoomModifyResponse modifyRoom(RoomModifyRequest request, long roomId) throws IllegalAccessException {
        Room room = roomRepository.findById(roomId).orElseThrow(IllegalAccessException::new);
        room.changeRoomName(request.roomName());
        room.changeSubject(request.topicId());

        return new RoomModifyResponse(room.getRoomName(), room.getTopicId());
    }

    private InGameUser findUser(long roomId, LoginUserRequest loginUserRequest) throws IllegalAccessException {
        User user = userRepository.findById(loginUserRequest.userId()).orElseThrow(IllegalAccessException::new);

        return new InGameUser(roomId, user.getId(), user.getEmail(), "user", false);
    }
}