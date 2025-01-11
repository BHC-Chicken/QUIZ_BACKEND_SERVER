package com.example.quiz.service;

import com.example.quiz.entity.User;
import com.example.quiz.enums.Role;
import com.example.quiz.jwt.JwtUtil;
import com.example.quiz.model.KakaoProfile;
import com.example.quiz.model.KakaoProperties;
import com.example.quiz.model.KakaoToken;
import com.example.quiz.model.OAuthToken;
import com.example.quiz.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final KakaoToken kakaoToken;
    private final UserRepository userRepository;
    private final KakaoProperties kakaoProperties;

    @Transactional
    public void kakaoCallback(String code, HttpServletResponse response) {
        OAuthToken oAuthToken = requestKakaoToken(code);
        KakaoProfile kakaoProfile = requestKakaoProfile(oAuthToken.getAccess_token());

        String username = kakaoProfile.getKakao_account().getEmail() + "_" + kakaoProfile.getId();
        String email = kakaoProfile.getKakao_account().getEmail();

        User user = findUser(username, email);

        String accessToken = JwtUtil.generateToken(user.getId(), user.getEmail());
        String refreshToken = JwtUtil.generateRefreshToken(user.getId());

        String encodeToken = URLEncoder.encode("Bearer " + accessToken, StandardCharsets.UTF_8);

        makeCookie(encodeToken, response);
    }

    @Transactional
    public void userUpdate(User user) {
        User persistence = userRepository.findById(user.getId()).orElseThrow(() -> new IllegalArgumentException("회원 찾기 실패"));

        persistence.changeEmail(user.getEmail());
    }

    private OAuthToken requestKakaoToken(String code) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", kakaoProperties.getAuthorizationGrantType());
        params.add("client_id", kakaoProperties.getClientId());
        params.add("redirect_uri", kakaoProperties.getRedirectUri());
        params.add("code", code);

        return sendHttpRequest(
                kakaoToken.getTokenUri(),
                HttpMethod.POST,
                httpHeaders,
                params,
                OAuthToken.class
        );
    }

    private KakaoProfile requestKakaoProfile(String accessToken) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        httpHeaders.add("Authorization", "Bearer " + accessToken);

        return sendHttpRequest(
                kakaoToken.getUserInfoUri(),
                HttpMethod.POST,
                httpHeaders,
                null,
                KakaoProfile.class
        );
    }

    private <T> T sendHttpRequest(String url, HttpMethod method, HttpHeaders headers, MultiValueMap<String, String> params, Class<T> responseType) {
        RestTemplate rt = new RestTemplate();
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);

        ResponseEntity<String> response = rt.exchange(
                url,
                method,
                requestEntity,
                String.class
        );

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(response.getBody(), responseType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse response: " + e.getMessage(), e);
        }
    }

    private void makeCookie(String token, HttpServletResponse response) {
        Cookie cookie = new Cookie("accessToken", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60);

        response.addCookie(cookie);
    }

    public User findUser(String username, String email) {
        return userRepository
                .findByUsername(username)
                .orElseGet(() -> userRepository.save(new User(username, email, Role.USER)));
    }
}
