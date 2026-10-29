package com.epam.edp.demo.service.auth;

import com.epam.edp.demo.dto.auth.SignInResponse;
import org.springframework.security.oauth2.core.user.OAuth2User;

public interface OAuth2UserService {
    SignInResponse socialLogin(OAuth2User oauthUser);

}
