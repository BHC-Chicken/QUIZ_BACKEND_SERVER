package com.example.quiz.entity;

import java.io.Serializable;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
public class Room implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roomId;
    private Long topicId;
    private String roomName;
    private Integer maxPeople;
    private Integer quizCount;
    @ColumnDefault("false")
    @Column(columnDefinition = "TINYINT(1)")
    private Boolean removeStatus;
    private String masterEmail;

    public void removeStatus() {
        this.removeStatus = true;
    }

    public void changeRoomName(String roomName) {
        if (roomName != null) {
            this.roomName = roomName;
        }
    }

    public void changeSubject(Long topicId) {
        if (topicId != null) {
            this.topicId = topicId;
        }
    }
}
