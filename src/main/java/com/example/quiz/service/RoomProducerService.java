package com.example.quiz.service;

import com.example.quiz.config.stompConfig.StompEventListener;
import com.example.quiz.dto.User.LoginUserRequest;
import com.example.quiz.dto.room.request.RoomCreateRequest;
import com.example.quiz.dto.room.response.RoomListResponse;
import com.example.quiz.dto.room.response.RoomResponse;
import com.example.quiz.entity.Game;
import com.example.quiz.entity.Room;
import com.example.quiz.entity.User;
import com.example.quiz.mapper.RoomMapper;
import com.example.quiz.repository.GameRepository;
import com.example.quiz.repository.RoomRepository;
import com.example.quiz.repository.UserRepository;
import com.example.quiz.vo.InGameUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashSet;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomProducerService {
    private static final int PAGE_SIZE = 10;

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final GameRepository gameRepository;
    private final StompEventListener stompEventListener;
    private final ApplicationEventPublisher eventPublisher;

    public RoomResponse createRoom(RoomCreateRequest roomRequest, LoginUserRequest loginUserRequest) throws IllegalAccessException {
        Room room = RoomMapper.INSTANCE.RoomCreateRequestToRoom(roomRequest, loginUserRequest.email());
        Room savedRoom = roomRepository.save(room);

        InGameUser masterUser = findUser(room.getRoomId(), loginUserRequest);
        Game game = gameRepository.save(
                new Game(String.valueOf(room.getRoomId()), 1, false, new HashSet<>()));
        game.getGameUser().add(masterUser);
        gameRepository.save(game);

        RoomResponse roomResponse = RoomMapper.INSTANCE.RoomToRoomResponse(savedRoom);
        eventPublisher.publishEvent(roomResponse);

        return roomResponse;
    }

    public Page<RoomListResponse> roomList(int index) {
        Pageable pageable = PageRequest.of(index, PAGE_SIZE, Sort.by("roomId").descending());

        return roomRepository.findAllByRemoveStatus(false, pageable)
                .map(room -> {
                    Integer currentPeople = stompEventListener.getSubscriptionCount(room.getRoomId()).get();

                    return RoomMapper.INSTANCE.RoomToRoomListResponse(room, currentPeople);
                });
    }

    private InGameUser findUser(long roomId, LoginUserRequest loginUserRequest) throws IllegalAccessException {
        User user = userRepository.findById(loginUserRequest.userId()).orElseThrow(IllegalAccessException::new);

        return new InGameUser(roomId, user.getId(), user.getEmail(), "master", false);
    }
}