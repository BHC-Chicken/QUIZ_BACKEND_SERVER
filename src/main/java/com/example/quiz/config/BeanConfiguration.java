package com.example.quiz.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@RequiredArgsConstructor
public class BeanConfiguration {

    @Bean
    public HashMap<Long, Long> alreadyInGameUser() {

        return new HashMap<>();
    }

    @Bean
    public HashMap<Long, AtomicInteger> roomSubscriptionCount() {

        return new HashMap<>();
    }
}
