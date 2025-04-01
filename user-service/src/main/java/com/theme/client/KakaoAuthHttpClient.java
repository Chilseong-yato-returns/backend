package com.theme.client;

import com.theme.dto.KakaoUserInfo;
import org.springframework.stereotype.Component;

@Component
public class KakaoAuthHttpClient {
    private String kakaoAccessTokenUrl;
    private String kakaoUserInfoUrl;

    public String getAuthCode(){
        //todo
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getAccessToken() {
        //todo
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public KakaoUserInfo getUserInfo(String accessToken) {
        //todo
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public KakaoUserInfo processLogin() {
        String accessToken = getAccessToken();
        return getUserInfo(accessToken);
    }
}
