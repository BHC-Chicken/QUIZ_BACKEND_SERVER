package com.example.quiz.config.stompConfig;

import com.example.quiz.config.BeanConfiguration;
import com.example.quiz.repository.RoomRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = BeanConfiguration.class)
class StompEventListenerTest {
    private final RoomRepository roomRepository = Mockito.mock(RoomRepository.class);
    private final ApplicationEventPublisher publisher = Mockito.mock(ApplicationEventPublisher.class);
    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;
    private final StompHeaderAccessorWrapper stompHeaderAccessorWrapper = Mockito.mock(StompHeaderAccessorWrapper.class);
    @Autowired
    private Map<String, String> sessionDestinationMap;
    @Autowired
    private Map<Long, AtomicInteger> roomSubscriptionCount;
    private StompEventListener stompEventListener;
    private final int TEST_THREAD = 30;

    @BeforeEach
    void setUp() {
        stompEventListener = new StompEventListener(
                roomRepository,
                stompHeaderAccessorWrapper,
                simpMessagingTemplate,
                sessionDestinationMap,
                roomSubscriptionCount,
                publisher
        );
    }

    @Test
    public void subscriptionTest() throws ExecutionException, InterruptedException {
        SessionSubscribeEvent event = Mockito.mock(SessionSubscribeEvent.class);
        StompHeaderAccessor accessor = Mockito.mock(StompHeaderAccessor.class);
        AtomicInteger callCount = new AtomicInteger(1);

        Mockito.when(stompHeaderAccessorWrapper.wrap(event))
                .thenReturn(accessor);

        Mockito.when(accessor.getSessionId())
                .thenReturn("user1", "user2", "user3", "user4", "user5", "user6", "user7", "user8", "user9", "user10",
                        "user11", "user12", "user13", "user14", "user15", "user16", "user17", "user18", "user19", "user20", "user21", "user22", "user23", "user24", "user25", "user26", "user27", "user28", "user29", "user30");

        Mockito.when(accessor.getDestination())
                .thenAnswer(i -> {
                    int call = callCount.getAndIncrement();

                    return (call % 2 == 1) ? "/room/1" : "/room/2";
                });

        roomSubscriptionCount.put(1L, new AtomicInteger(1));
        roomSubscriptionCount.put(2L, new AtomicInteger(1));


        List<CompletableFuture<Void>> list = new ArrayList<>();

        CyclicBarrier barrier = new CyclicBarrier(TEST_THREAD);

        for (int i = 1; i <= TEST_THREAD; i++) {
            list.add(CompletableFuture.runAsync(
                    () -> {
                        try {
                            barrier.await();
                            stompEventListener.handleSessionSubscribeEvent(event);
                        } catch (RuntimeException | IllegalAccessException e) {
                            log.info(e.getMessage());
                        } catch (BrokenBarrierException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
            ));
        }

        CompletableFuture.allOf(list.toArray(new CompletableFuture[0])).get();

        Assertions.assertEquals(8, roomSubscriptionCount.get(1L).get());
        Assertions.assertEquals(8, roomSubscriptionCount.get(2L).get());
    }

    @Test
    public void unsubscriptionTest() throws IllegalAccessException, ExecutionException, InterruptedException {
        SessionUnsubscribeEvent event = Mockito.mock(SessionUnsubscribeEvent.class);
        StompHeaderAccessor accessor = Mockito.mock(StompHeaderAccessor.class);
        AtomicInteger callCount = new AtomicInteger(1);

        Mockito.when(stompHeaderAccessorWrapper.wrap(event))
                .thenReturn(accessor);

        Mockito.when(accessor.getSessionId())
                .thenReturn("user1", "user2", "user3", "user4", "user5", "user6", "user7", "user8", "user9", "user10",
                        "user11", "user12", "user13", "user14");

        Mockito.when(accessor.getDestination())
                .thenAnswer(i -> {
                    int call = callCount.getAndIncrement();

                    return (call % 2 == 1) ? "/room/1" : "/room/2";
                });

        roomSubscriptionCount.put(1L, new AtomicInteger(8));
        roomSubscriptionCount.put(2L, new AtomicInteger(8));

        for (int i = 1; i <= 14; i++) {
            if (i % 2 == 1) {
                sessionDestinationMap.put("user" + i, "/room/1");
            } else {
                sessionDestinationMap.put("user" + i, "/room/2");
            }
        }

        List<CompletableFuture<Void>> list = new ArrayList<>();

        CyclicBarrier barrier = new CyclicBarrier(14);

        for (int i = 1; i <= 14; i++) {
            list.add(CompletableFuture.runAsync(
                    () -> {
                        try {
                            barrier.await();
                            stompEventListener.handleSessionUnsubscribeEvent(event);
                        } catch (RuntimeException e) {
                            log.info(e.getMessage());
                        } catch (BrokenBarrierException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
            ));
        }

        CompletableFuture.allOf(list.toArray(new CompletableFuture[0])).get();

        Assertions.assertEquals(1, roomSubscriptionCount.get(1L).get());
        Assertions.assertEquals(1, roomSubscriptionCount.get(2L).get());
    }

    @Test
    public void whenUserAttemptsToSubscribeToFullRoom_thenThrowIllegalAccessException() {
        SessionSubscribeEvent event = Mockito.mock(SessionSubscribeEvent.class);
        StompHeaderAccessor accessor = Mockito.mock(StompHeaderAccessor.class);

        Mockito.when(stompHeaderAccessorWrapper.wrap(event)).thenReturn(accessor);
        Mockito.when(accessor.getSessionId()).thenReturn("testSessionId");
        Mockito.when(accessor.getDestination()).thenReturn("/room/1");

        roomSubscriptionCount.put(1L, new AtomicInteger(8));

        Assertions.assertThrows(RuntimeException.class,
                () -> stompEventListener.handleSessionSubscribeEvent(event));
    }

    @Test
    public void whenUserSubscribesToRoomSuccessfully_thenSessionAndCountUpdated() throws IllegalAccessException {
        SessionSubscribeEvent event = Mockito.mock(SessionSubscribeEvent.class);
        StompHeaderAccessor accessor = Mockito.mock(StompHeaderAccessor.class);

        Mockito.when(stompHeaderAccessorWrapper.wrap(event)).thenReturn(accessor);
        Mockito.when(accessor.getSessionId()).thenReturn("testSession1");
        Mockito.when(accessor.getDestination()).thenReturn("/room/1");

        roomSubscriptionCount.put(1L, new AtomicInteger(0));

        stompEventListener.handleSessionSubscribeEvent(event);

        Assertions.assertEquals(1, roomSubscriptionCount.get(1L).get());
        Assertions.assertEquals("/room/1", sessionDestinationMap.get("testSession1"));
    }

    @Test
    public void whenUserSubscribesToFullRoom_thenExceptionThrown() {
        SessionSubscribeEvent event = Mockito.mock(SessionSubscribeEvent.class);
        StompHeaderAccessor accessor = Mockito.mock(StompHeaderAccessor.class);

        Mockito.when(stompHeaderAccessorWrapper.wrap(event)).thenReturn(accessor);
        Mockito.when(accessor.getSessionId()).thenReturn("testSessionId");
        Mockito.when(accessor.getDestination()).thenReturn("/room/1");

        roomSubscriptionCount.put(1L, new AtomicInteger(8)); // Simulate room full

        Assertions.assertThrows(RuntimeException.class,
                () -> stompEventListener.handleSessionSubscribeEvent(event));
    }

    @Test
    public void whenUserAttemptsDuplicateSubscription_thenExceptionThrown() {
        SessionSubscribeEvent event = Mockito.mock(SessionSubscribeEvent.class);
        StompHeaderAccessor accessor = Mockito.mock(StompHeaderAccessor.class);

        Mockito.when(stompHeaderAccessorWrapper.wrap(event)).thenReturn(accessor);
        Mockito.when(accessor.getSessionId()).thenReturn("testSession1");
        Mockito.when(accessor.getDestination()).thenReturn("/room/1");

        sessionDestinationMap.put("testSession1", "/room/1");

        Assertions.assertThrows(IllegalAccessException.class,
                () -> stompEventListener.handleSessionSubscribeEvent(event));
    }

    @Test
    public void whenMultipleSubscriptionsToDifferentRooms_thenCountsUpdatedCorrectly() throws IllegalAccessException {
        long roomId1 = 1L;
        long roomId2 = 2L;

        SessionSubscribeEvent event1 = Mockito.mock(SessionSubscribeEvent.class);
        StompHeaderAccessor accessor1 = Mockito.mock(StompHeaderAccessor.class);
        Mockito.when(stompHeaderAccessorWrapper.wrap(event1)).thenReturn(accessor1);
        Mockito.when(accessor1.getSessionId()).thenReturn("session1");
        Mockito.when(accessor1.getDestination()).thenReturn("/room/1");

        SessionSubscribeEvent event2 = Mockito.mock(SessionSubscribeEvent.class);
        StompHeaderAccessor accessor2 = Mockito.mock(StompHeaderAccessor.class);
        Mockito.when(stompHeaderAccessorWrapper.wrap(event2)).thenReturn(accessor2);
        Mockito.when(accessor2.getSessionId()).thenReturn("session2");
        Mockito.when(accessor2.getDestination()).thenReturn("/room/2");

        roomSubscriptionCount.put(roomId1, new AtomicInteger(0));
        roomSubscriptionCount.put(roomId2, new AtomicInteger(0));

        stompEventListener.handleSessionSubscribeEvent(event1);
        stompEventListener.handleSessionSubscribeEvent(event2);

        Assertions.assertEquals(1, roomSubscriptionCount.get(roomId1).get());
        Assertions.assertEquals(1, roomSubscriptionCount.get(roomId2).get());
        Assertions.assertEquals("/room/1", sessionDestinationMap.get("session1"));
        Assertions.assertEquals("/room/2", sessionDestinationMap.get("session2"));
    }
}