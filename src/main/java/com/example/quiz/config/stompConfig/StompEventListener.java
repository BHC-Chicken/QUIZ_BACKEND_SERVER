package com.example.quiz.config.stompConfig;

import com.example.quiz.dto.response.CurrentOccupancy;
import com.example.quiz.dto.room.ChangeCurrentOccupancies;
import com.example.quiz.dto.room.response.RoomResponse;
import com.example.quiz.entity.Room;
import com.example.quiz.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompEventListener {
    private static final String ROOM_PREFIX = "/room/";

    private final RoomRepository roomRepository;
    private final StompHeaderAccessorWrapper headerAccessorService;
    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, String> sessionDestinationMap;
    private final Map<Long, AtomicInteger> roomSubscriptionCount;
    private final ApplicationEventPublisher publisher;

    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) throws IllegalAccessException {
        StompHeaderAccessor accessor = headerAccessorService.wrap(event);
        String sessionId = accessor.getSessionId();
        String destination = extractDestinationFromEvent(accessor);

        if (isRoomDestination(destination)) {
            if (sessionDestinationMap.containsKey(sessionId)) {
                throw new IllegalAccessException();
            }

            long roomId = getRoomId(destination);
            AtomicInteger subscriptionCount = roomSubscriptionCount.computeIfAbsent(roomId, id -> new AtomicInteger(0));
            int currentCount = subscriptionCount.updateAndGet(current -> {
                if (current >= 8) {
                    throw new RuntimeException("Room capacity reached : " + roomId);
                }
                return current + 1;
            });

            sessionDestinationMap.put(sessionId, destination);
            publisher.publishEvent(new ChangeCurrentOccupancies(roomId, currentCount));
        }
    }

    @EventListener
    @Transactional
    public void handleSessionUnsubscribeEvent(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = headerAccessorService.wrap(event);
        String sessionId = accessor.getSessionId();
        String destination = sessionDestinationMap.remove(sessionId);

        long roomId = getRoomId(destination);
        AtomicInteger count = roomSubscriptionCount.get(roomId);

        if (count != null) {
            int currentCount = count.updateAndGet(current -> {
                if (current < 1) {
                    throw new RuntimeException("Room capacity reached : " + roomId);
                }

                return current - 1;
            });

            publisher.publishEvent(new ChangeCurrentOccupancies(roomId, currentCount));

            if (currentCount <= 0) {
                roomSubscriptionCount.remove(roomId);
                Optional<Room> room = roomRepository.findById(roomId);
                room.ifPresent(Room::removeStatus);
            }
        }
    }

    @EventListener
    public void createNewRoomEvent(RoomResponse response) {

        messagingTemplate.convertAndSend("/pub/occupancy", response);
    }

    private String extractDestinationFromEvent(StompHeaderAccessor accessor) {
        return accessor.getDestination();
    }

    private boolean isRoomDestination(String destination) {
        return destination != null && destination.startsWith(ROOM_PREFIX);
    }

    @EventListener
    public void broadcastCurrentOccupancy(ChangeCurrentOccupancies roomInfo) {

        messagingTemplate.convertAndSend("/pub/occupancy", new CurrentOccupancy(roomInfo.roomId(), roomInfo.currentPeople()));
    }

    public AtomicInteger getSubscriptionCount(long roomId) {
        return roomSubscriptionCount.getOrDefault(roomId, new AtomicInteger(0));
    }

    private long getRoomId(String destination) {
        if (destination == null || !destination.startsWith(ROOM_PREFIX)) {
            throw new IllegalArgumentException("Invalid destination: " + destination);
        }

        String[] url = destination.split("/");
        return Long.parseLong(url[2]);
    }
}