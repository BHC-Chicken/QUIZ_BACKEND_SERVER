package com.example.quiz.entity;

import com.example.quiz.enums.Role;
import jakarta.persistence.*;
import lombok.*;


@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; 

    @Column(nullable = false, length = 100, unique = true)
    private String username;

    @Column(nullable = false, length = 50)
    private String email;

    @Enumerated(value = EnumType.STRING)
    private Role role;

    private boolean readyStatus;

    public User(Long userId, Role userRole, boolean readyStatus) {
        this.id = userId;
        this.role = userRole;
        this.readyStatus = readyStatus;
    }

    public void changeUserReadyStatus(boolean readyStatus) {
        this.readyStatus = readyStatus;
    }
    public void changeUserName(String username) {
        this.username = username;
    }
}
