package com.example.quiz.config.stompConfig;

import com.example.quiz.dto.User.LoginUserRequest;
import com.example.quiz.dto.response.CurrentOccupancy;
import com.example.quiz.dto.room.ChangeCurrentOccupancies;
import com.example.quiz.dto.room.response.RoomResponse;
import com.example.quiz.entity.Game;
import com.example.quiz.entity.Room;
import com.example.quiz.entity.User;
import com.example.quiz.enums.Role;
import com.example.quiz.repository.GameRepository;
import com.example.quiz.repository.RoomRepository;
import com.example.quiz.repository.UserRepository;
import com.example.quiz.vo.InGameUser;
import com.mongodb.internal.VisibleForTesting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompEventListener {
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final RoomRepository roomRepository;
    private final ApplicationEventPublisher publisher;
    private final SimpMessagingTemplate messagingTemplate;
    private final StompHeaderAccessorWrapper headerAccessorService;

    private final Map<Long, Long> alreadyInGameUser;
    private final Map<Long, AtomicInteger> roomSubscriptionCount;

    @EventListener
    @Transactional
    public void handleSessionUnsubscribeEvent(SessionUnsubscribeEvent event) throws IllegalAccessException {
        LoginUserRequest loginUserRequest = extractLoginUser(event);
        Long roomId = removeUserFromRoomMapping(loginUserRequest.userId());
        Game game = findGameByRoomId(roomId);

        removeUserFromGame(game, loginUserRequest.userId(), roomId);
        updateRoomSubscriptionCount(roomId);
    }

    @EventListener
    public void createNewRoomEvent(RoomResponse response) {
        messagingTemplate.convertAndSend("/pub/occupancy", response);
    }

    @EventListener
    public void broadcastCurrentOccupancy(ChangeCurrentOccupancies roomInfo) {
        messagingTemplate.convertAndSend("/pub/occupancy", new CurrentOccupancy(roomInfo.roomId(), roomInfo.currentPeople()));
    }

    private LoginUserRequest extractLoginUser(SessionUnsubscribeEvent event) throws IllegalAccessException {
        StompHeaderAccessor accessor = headerAccessorService.wrap(event);
        LoginUserRequest loginUserRequest = (LoginUserRequest) accessor.getSessionAttributes().get("loginUser");

        if (loginUserRequest == null) {
            throw new IllegalAccessException("Login user is null in session attributes");
        }

        log.info("unsub session: {}", loginUserRequest.userId());

        return loginUserRequest;
    }

    private Long removeUserFromRoomMapping(Long userId) {
        Long roomId = alreadyInGameUser.remove(userId);

        if (roomId == null) {
            throw new IllegalStateException("User is not associated with any game");
        }

        return roomId;
    }

    private Game findGameByRoomId(Long roomId) {
        return gameRepository.findById(String.valueOf(roomId))
                .orElseThrow(() -> new IllegalStateException("Game not found for roomId: " + roomId));
    }

    private void removeUserFromGame(Game game, Long userId, Long roomId) throws IllegalAccessException {
        InGameUser inGameUser = findUser(userId, roomId);
        game.getGameUser().remove(inGameUser);
        gameRepository.save(game);
    }

    private void updateRoomSubscriptionCount(Long roomId) {
        AtomicInteger count = roomSubscriptionCount.get(roomId);

        if (count == null) {
            log.warn("Room Id {} subscription count is null", roomId);

            return;
        }

        int currentCount = count.updateAndGet(current -> {
            if (current < 1) {
                throw new RuntimeException("Room capacity cannot be negative for roomId: " + roomId);
            }

            return current - 1;
        });

        log.info("current people: {}", currentCount);
        publisher.publishEvent(new ChangeCurrentOccupancies(roomId, currentCount));

        if (currentCount <= 0) {
            cleanUpEmptyRoom(roomId);
        }
    }

    private void cleanUpEmptyRoom(Long roomId) {
        roomSubscriptionCount.remove(roomId);
        roomRepository.findById(roomId).ifPresent(Room::removeStatus);
    }

    private InGameUser findUser(long userId, long roomId) throws IllegalAccessException {
        User user = userRepository.findById(userId).orElseThrow(IllegalAccessException::new);

        return new InGameUser(roomId, user.getId(), user.getEmail(), Role.USER, false, false);
    }
}