package com.example.quiz.service;

import com.example.quiz.dto.request.RequestAnswer;
import com.example.quiz.dto.request.RequestRemainQuiz;
import com.example.quiz.dto.response.ResponseCheckQuiz;
import com.example.quiz.dto.response.ResponseReadyGame;
import com.example.quiz.dto.response.ResponseQuiz;
import com.example.quiz.dto.response.ResponseStartGame;
import com.example.quiz.entity.Game;
import com.example.quiz.entity.Quiz;
import com.example.quiz.entity.Room;
import com.example.quiz.entity.user.User;
import com.example.quiz.enums.Role;
import com.example.quiz.repository.GameRepository;
import com.example.quiz.repository.QuizRepository;
import com.example.quiz.repository.RoomRepository;
import com.example.quiz.repository.UserRepository;
import com.example.quiz.vo.InGameUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
@Service
public class GameService {

    private static final Map<Long, List<Long>> roomQuizMap = new ConcurrentHashMap<>();
    private static final Map<Long, Map<Long, Long>> currentInGameScore = new ConcurrentHashMap<>();
    private static final Map<Long, Integer> remainQuizMap = new ConcurrentHashMap<>();
    private final GameRepository gameRepository;
    private final QuizRepository quizRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    @Transactional
    public ResponseReadyGame toggleReadyStatus(String roomId, Long userId) {
        // User, Game 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Game game = gameRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        // 현재 로그인한 InGameUser 반환
        Set<InGameUser> inGameUserSet = game.getGameUser();
        // TODO Optional 반환값으로 변환
        InGameUser currentUser = findUser(inGameUserSet, userId);

        // 준비상태 토글
        toggle(game, currentUser);
        // User 준비상태 따라서 DTO 반환
        return handleReadyStatus(user, currentUser, inGameUserSet);
    }

    private InGameUser findUser(Set<InGameUser> inGameUserSet, long userId) {
        for(InGameUser inGameUser : inGameUserSet) {
            if(inGameUser.getId() == userId) {
                return inGameUser;
            }
        }
        return null;
    }

    private void toggle(Game game, InGameUser inGameUser) {
        // 준비상태 변화 및 게임방 최신화
        inGameUser.changeReadyStatus(!inGameUser.isReadyStatus());
        game.getGameUser().add(inGameUser);
        gameRepository.save(game);
    }
    // User
    private ResponseReadyGame handleReadyStatus(User user, InGameUser inGameUser, Set<InGameUser> inGameUserSet) {
        if(isAllReady(inGameUserSet)) {
            return new ResponseReadyGame(user.getId(), user.getEmail(), user.getRole(), inGameUser.isReadyStatus(), true);
        }
        else {
            return new ResponseReadyGame(user.getId(), user.getEmail(), user.getRole(), inGameUser.isReadyStatus(), false);
        }
    }
    // User 인 사람이 모두 Ready 인지 판단
    private boolean isAllReady(Set<InGameUser> inGameUserSet) {
        for(InGameUser inGameUser : inGameUserSet) {
            // Admin 통과
            if(!isUser(inGameUser)) {
                continue;
            }
            if(!inGameUser.isReadyStatus()) {
                return false;
            }
        }
        return true;
    }

    private boolean isUser(InGameUser inGameUser) {
        return inGameUser.getRole() == Role.USER;
    }

    @Transactional
    public ResponseStartGame startGame(String roomId, RequestRemainQuiz requestRemainQuiz) {
        Room room = roomRepository.findById(Long.parseLong(roomId)).orElseThrow(() -> new RuntimeException("Room not found"));
        remainQuizMap.put(Long.parseLong(roomId), room.getQuizCount());
        room.changeQuizCount(requestRemainQuiz.remainQuiz());

        return new ResponseStartGame(room.getQuizCount());
    }

    @Transactional
    public ResponseQuiz sendQuiz(String roomId) {
        Room room = roomRepository.findById(Long.valueOf(roomId)).orElseThrow(() -> new RuntimeException("Room not found"));
        Quiz quiz = selectRandomQuiz(Long.parseLong(roomId), room.getTopicId());

        remainQuizMap.merge(Long.parseLong(roomId), 1, (oldValue, newValue) -> oldValue - 1);
        makeGame(Long.parseLong(roomId));

        return new ResponseQuiz(quiz.getProblem(), quiz.getCorrectAnswer(), quiz.getDescription());
    }

    private void makeGame(Long roomId) {
        if(remainQuizMap.get(roomId) == 0) {
            roomRepository.findById(roomId).ifPresent(Room::removeStatus);
            Game game = new Game(String.valueOf(roomId), roomId, 0, false, new HashSet<>());
            gameRepository.save(game);
        }
    }

    // topic Id 맞게 중복되지 않는 Quiz 반환
    public Quiz selectRandomQuiz(Long roomId, Long topicId) {
        roomQuizMap.putIfAbsent(roomId, new ArrayList<>());
        List<Long> usedQuizIds = roomQuizMap.get(roomId);

        List<Quiz> allQuizzes = quizRepository.findAllByTopicId(topicId);
        List<Quiz> availableQuizzes = quizRepository.findAllByTopicId(topicId).stream()
                .filter(quiz -> !usedQuizIds.contains(quiz.getQuizId()))
                .toList();

        // 사용 가능한 문제가 없으면 모든 문제를 다시 사용 가능하도록 초기화
        if (availableQuizzes.isEmpty()) {
            log.info("문제를 다 풀었습니다. 문제집을 초기화 합니다.");
            usedQuizIds.clear();
            availableQuizzes = allQuizzes;
        }

        int randomIndex = new Random().nextInt(availableQuizzes.size());
        Quiz selectedQuiz = availableQuizzes.get(randomIndex);

        usedQuizIds.add(selectedQuiz.getQuizId());
        return selectedQuiz;
    }

    public ResponseCheckQuiz checkAnswer(String id, RequestAnswer requestAnswer) {
        User user = userRepository.findById(requestAnswer.userId()).orElseThrow(() -> new RuntimeException("User not found"));
        Room room = roomRepository.findById(Long.valueOf(id)).orElseThrow(() -> new RuntimeException("Room not found"));
        Long correctQuizId = correctQuizId(roomQuizMap.get(room.getRoomId()));
        Quiz quiz = quizRepository.findById(correctQuizId).orElseThrow(() -> new RuntimeException("Quiz not found"));
        boolean isRight = check(requestAnswer.answer(), quiz);

        // 정답이 맞으면 정답 반환. 오답이면 null 반환.
        if(isRight) {
            increaseScore(user.getId(), room.getRoomId());
            // final winner 반환
            List<String> finalWinners = findFinalWinners(room.getRoomId());
            if(requestAnswer.finalQuiz()) {
                removeInGameInfo(room.getRoomId());
                return new ResponseCheckQuiz(user.getEmail(), true, true, finalWinners, quiz.getCorrectAnswer(), quiz.getDescription());
            }
            else {
                return new ResponseCheckQuiz(user.getEmail(), true, false, finalWinners, quiz.getCorrectAnswer(), quiz.getDescription());
            }
        }
        else {
            return new ResponseCheckQuiz(user.getEmail(), false, false,null, quiz.getCorrectAnswer(), quiz.getDescription());
        }
    }

    private boolean check(String answer, Quiz quiz) {
        return quiz.getCorrectAnswer().equals(answer);
    }
    // 게임이 끝난 후 인게임 정보 삭제
    private void removeInGameInfo(Long roomId) {
        currentInGameScore.remove(roomId);
        roomQuizMap.remove(roomId);
    }

    private Long correctQuizId(List<Long> usedQuizIds) {
        return usedQuizIds.stream()
                .skip(usedQuizIds.size() - 1)
                .findFirst()
                .orElse(-1L);
    }
    // 방이 없으면 추가하고, 점수 카운팅을 한다
    private void increaseScore(Long userId, Long roomId) {
        currentInGameScore.putIfAbsent(roomId, new HashMap<>());
        Map<Long, Long> score = currentInGameScore.get(roomId);
        score.put(userId, score.getOrDefault(userId, 0L) + 1L);
    }
    // 최종 우승자 반환
    private List<String> findFinalWinners(Long roomId) {
        Map<Long, Long> score = currentInGameScore.get(roomId);
        if (score == null || score.isEmpty()) {
            return Collections.emptyList();
        }

        // 1) 최대 점수 찾기
        Long maxScore = score.values().stream()
                .max(Long::compare)
                .orElse(Long.MIN_VALUE);

        // 2) 최대 점수를 가진 userId들 필터 & 이메일 변환
        return score.entrySet().stream()
                // 최대 점수를 가진 엔트리만 추출
                .filter(e -> e.getValue().equals(maxScore))
                // userId 추출
                .map(Map.Entry::getKey)
                // userId -> email 변환 (UserRepository 예시)
                .map(userId -> userRepository.findById(userId)
                        .map(User::getEmail)
                        .orElse("unknown@example.com"))  // 존재하지 않는 사용자 처리
                .toList();
    }
}
