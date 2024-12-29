package com.example.quiz.vo;

public class InGameUser {
    private final Long roomId;
    private final Long id;
    private final String username;
    private final String role;
    private boolean readyStatus;

    public InGameUser(Long id, Long roomId, String username, String role, boolean readyStatus) {
        this.id = id;
        this.roomId = roomId;
        this.username = username;
        this.role = role;
        this.readyStatus = readyStatus;
    }

    public void changeReadyStatus(boolean readyStatus) {
        this.readyStatus = readyStatus;
    }
}
