package com.theme.service;

import com.theme.auth.JwtUtils;
import com.theme.client.KakaoAuthHttpClient;
import com.theme.domain.User;
import com.theme.dto.UserAuthResponse;
import com.theme.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

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
    public Mono<UserAuthResponse> processKakaoAuth(String authCode) {
        return kakaoAuthHttpClient.processLogin(authCode)
                .flatMap(userInfo ->{
                    User user = userRepository.findById(userInfo.getEmail()).orElse(null);
                    if(user == null) user = userRepository.save(userInfo.toEntity());
                    return Mono.just(user);
                })
                .map(user -> {
                    String accessToken = jwtUtils.generateAccessToken(user.getUserEmail());
                    String refreshToken = jwtUtils.generateRefreshToken(user.getUserEmail());
                    return UserAuthResponse.builder()
                            .accessToken(accessToken)
                            .refreshToken(refreshToken)
                            .build();
                });
    }
}
