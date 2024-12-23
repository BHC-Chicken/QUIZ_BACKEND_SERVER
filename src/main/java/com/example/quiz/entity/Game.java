package com.example.quiz.entity;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Document(collation = "game")
@Builder
@Getter
public class Game {
    @Id
    @Column(nullable = false)
    private String id;
    private Integer currentParticipantsNo;
    private Boolean isGaming;
    @Field("gameUser")
    private Set<User> gameUser;

    public void changeGameStatus(boolean status) {
        this.isGaming = status;
    }
}
