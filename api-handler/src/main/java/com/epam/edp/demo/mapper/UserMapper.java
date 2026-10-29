package com.epam.edp.demo.mapper;

import com.epam.edp.demo.dto.auth.SignInResponse;
import com.epam.edp.demo.entity.auth.CustomUserDetails;
import com.epam.edp.demo.entity.auth.Role;
import com.epam.edp.demo.exception.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;

@Slf4j
public class UserMapper {

    private UserMapper(){}

    public static SignInResponse toSignInResponse(CustomUserDetails user, String token, String pictureRetrieveUrl) {
        GrantedAuthority grantedAuthority = user.getAuthorities().stream().findFirst()
                .orElseThrow(() -> {
                    log.warn("Automatic role assignment failed. Role for user %s not found.");
                    return new UserNotFoundException(String.format("Role for user %s not found", user.getUsername()));
                });

        Role role = Role.fromAuthority(grantedAuthority.getAuthority());
        return SignInResponse.builder()
                .token(token)
                .role(role.name())
                .userName(user.getUserUsername())
                .email(user.getEmail())
                .imageUrl(pictureRetrieveUrl)
                .build();
    }
}
