package com.epam.edp.demo.service.auth;

import com.epam.edp.demo.dto.auth.SignInResponse;
import com.epam.edp.demo.entity.auth.*;
import com.epam.edp.demo.mapper.UserMapper;
import com.epam.edp.demo.repository.auth.UserRepository;
import com.epam.edp.demo.repository.staff.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OAuth2UserServiceImpl implements OAuth2UserService {
    private final JwtService jwtService;
    private final StaffRepository staffRepository;
    private final UserRepository userRepository;

    @Override
    public SignInResponse socialLogin(OAuth2User oauthUser) {
        String email = Objects.requireNonNull(oauthUser.getAttribute("email")).toString().trim().toLowerCase();

        Map<String, Object> attributes = oauthUser.getAttributes();
        String pictureUrl = (String) attributes.getOrDefault("picture", null);

        log.info("Processing social login for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    log.info("User not found in database, creating new user for email: {}", email);
                    return createGoogleUser(oauthUser);
                });

        CustomUserDetails userDetails = new CustomUserDetails(user);

        String jwt = jwtService.generateToken(userDetails);

        log.info("Generated JWT for user {}: {}", email, jwt);
        return UserMapper.toSignInResponse(userDetails, jwt, pictureUrl);
    }

    private User createGoogleUser(OAuth2User oauthUser) {

        String email = Objects.requireNonNull(oauthUser.getAttribute("email"))
                .toString().trim().toLowerCase();

        String firstName = oauthUser.getAttribute("given_name");

        String fullName = oauthUser.getAttribute("name");

        String lastName = oauthUser.getAttribute("family_name");

        if (firstName == null && fullName != null) {
            firstName = fullName;
        }

        Role role = findRole(email);

        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .email(email)
                .firstName(firstName != null ? firstName : "UNKNOWN")
                .lastName(lastName)
                .role(role)
//                .authProvider(AuthProvider.GOOGLE)
                .build();

        return userRepository.save(user);
    }

    public Role findRole(String email) {
        log.info("Looking up role for user {}", email);
        Role role = Role.CUSTOMER;

        if (staffRepository.staffExists(email, Role.TRAVEL_AGENT)) {
            role = Role.TRAVEL_AGENT;
        } else if (staffRepository.staffExists(email, Role.ADMIN)) {
            log.warn("ADMIN should not login with external service. Customer role for {}!", email);
        }

        log.info("Found role {} for user {}", role, email);
        return role;
    }
}
