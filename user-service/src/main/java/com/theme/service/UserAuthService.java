package com.theme.service;

import com.theme.auth.JwtUtils;
import com.theme.client.KakaoAuthHttpClient;
import com.theme.domain.User;
import com.theme.dto.KakaoUserInfo;
import com.theme.dto.UserAuthResponse;
import com.theme.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAuthService {
    private final UserRepository userRepository;
    private final KakaoAuthHttpClient kakaoAuthHttpClient;
    private final JwtUtils jwtUtils;
    public UserAuthService(
            UserRepository userRepository,
            KakaoAuthHttpClient kakaoAuthHttpClient,
            JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.kakaoAuthHttpClient = kakaoAuthHttpClient;
        this.jwtUtils = jwtUtils;
    }
    /**
     * 카카오 로그인 및 회원가입
    * */
    @Transactional
    public UserAuthResponse kakaoLogin() {
        KakaoUserInfo kakaoUserInfo = kakaoAuthHttpClient.processLogin();
        if(userRepository.findById(kakaoUserInfo.getEmail()).orElse(null) == null) {
            userRepository.save(User.builder()
                .userEmail(kakaoUserInfo.getEmail())
                .profileImg(kakaoUserInfo.getProfileImage())
                .build());
        }
        String serviceAccessToken = jwtUtils.generateAccessToken(kakaoUserInfo.getEmail());
        String serviceRefreshToken = jwtUtils.generateRefreshToken(kakaoUserInfo.getEmail());
        return UserAuthResponse.builder()
                .accessToken(serviceAccessToken)
                .refreshToken(serviceRefreshToken)
                .build();
    }
}
