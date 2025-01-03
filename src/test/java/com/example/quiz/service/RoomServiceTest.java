package com.example.quiz.service;

import com.example.quiz.dto.User.LoginUserRequest;
import com.example.quiz.dto.room.request.RoomCreateRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class RoomServiceTest {

    @Autowired
    private RoomService roomService;
    @Autowired
    private Map<Long, Long> alreadyInGameUser;
    @Autowired
    private Map<Long, AtomicInteger> roomSubscriptionCount;
    @Autowired
    private RoomProducerService roomProducerService;
    @Autowired
    private UserService userService;

    private final int TEST_THREAD = 100;

    @Container
    private static final MySQLContainer<?> mysqlContainer = new MySQLContainer<>(DockerImageName.parse("mysql:latest"))
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withInitScript("init.sql");

    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:latest");

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");

        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Test
    public void subscriptionTest() throws ExecutionException, InterruptedException, IllegalAccessException {
        AtomicLong roomNumber = new AtomicLong(1);
        AtomicLong userNumber = new AtomicLong(1);
        AtomicInteger sessionNumber = new AtomicInteger(1);

        for (int i = 0; i < 100; i++) {
            userService.findUser("user" + i, "email" + i);
        }

        roomSubscriptionCount.put(1L, new AtomicInteger(1));
        roomSubscriptionCount.put(2L, new AtomicInteger(1));

        roomProducerService.createRoom(new RoomCreateRequest("room1", 1L, 8, 8), new LoginUserRequest(1L, "email"));
        roomProducerService.createRoom(new RoomCreateRequest("room2", 1L, 8, 8), new LoginUserRequest(2L, "email"));

        List<CompletableFuture<Void>> list = new ArrayList<>();

        CyclicBarrier barrier = new CyclicBarrier(98);

        for (long i = 3; i <= TEST_THREAD; i++) {
            list.add(CompletableFuture.runAsync(
                    () -> {
                        try {
                            barrier.await();

                            roomService.enterRoom(roomNumber.getAndIncrement() % 2 + 1, new LoginUserRequest(userNumber.getAndIncrement(), "email"));
                        } catch (RuntimeException | IllegalAccessException e) {
                            log.info("user: {}", e.getMessage());
                        } catch (BrokenBarrierException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
            ));
        }

        CompletableFuture.allOf(list.toArray(new CompletableFuture[0])).get();

        Assertions.assertEquals(15, roomSubscriptionCount.get(1L).get());
        Assertions.assertEquals(15, roomSubscriptionCount.get(2L).get());
    }
}