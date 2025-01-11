package com.example.quiz.vo;

import com.example.quiz.enums.Role;
import lombok.Getter;

import java.util.Objects;

@Getter
public class InGameUser {
    private final Long id;
    private final Long roomId;
    private final String username;
    private final Role role;
    private boolean isReadyStatus;

    public InGameUser(Long id, Long roomId, String username, Role role, boolean isReadyStatus) {
        this.id = id;
        this.roomId = roomId;
        this.username = username;
        this.role = role;
        this.isReadyStatus = isReadyStatus;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        InGameUser that = (InGameUser) object;
        return Objects.equals(id, that.id) && Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username);
    }

    public void changeReadyStatus(boolean readyStatus) {
        this.isReadyStatus = readyStatus;
    }
}
