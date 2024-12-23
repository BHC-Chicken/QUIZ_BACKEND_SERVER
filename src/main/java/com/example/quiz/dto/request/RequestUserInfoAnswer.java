package com.example.quiz.dto.request;

import java.util.List;

public record RequestUserInfoAnswer(Long userId, List<Long> questionList, Integer quizCount) {
}
