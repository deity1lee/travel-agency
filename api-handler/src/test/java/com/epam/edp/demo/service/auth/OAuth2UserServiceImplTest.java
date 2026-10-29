package com.epam.edp.demo.service.auth;

import com.epam.edp.demo.dto.auth.SignInResponse;
import com.epam.edp.demo.entity.auth.Role;
import com.epam.edp.demo.entity.auth.User;
import com.epam.edp.demo.repository.auth.UserRepository;
import com.epam.edp.demo.repository.staff.StaffRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2UserServiceImplTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OAuth2UserServiceImpl service;

    @Test
    void socialLogin_existingUser_returnsMappedResponseWithPicture() {
        OAuth2User oauthUser = mockOAuth2User();
        when(oauthUser.getAttribute("email")).thenReturn("foo@bar.com");
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "foo@bar.com");
        attributes.put("picture", "http://pic.example/img.png");
        when(oauthUser.getAttributes()).thenReturn(attributes);

        User existing = new User("user-123", "foo@bar.com", "HASH", "John", "Doe", Role.CUSTOMER);
        when(userRepository.findByEmail("foo@bar.com")).thenReturn(Optional.of(existing));
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        SignInResponse response = service.socialLogin(oauthUser);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.email()).isEqualTo("foo@bar.com");
        assertThat(response.imageUrl()).isEqualTo("http://pic.example/img.png");
        verify(userRepository, never()).save(any());
    }

    @Test
    void socialLogin_newUser_createsGoogleUserWithGivenName() {
        OAuth2User oauthUser = mockOAuth2User();
        when(oauthUser.getAttribute("email")).thenReturn("new@bar.com");
        when(oauthUser.getAttribute("given_name")).thenReturn("John");
        when(oauthUser.getAttribute("name")).thenReturn(null);
        when(oauthUser.getAttribute("family_name")).thenReturn("Doe");
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "new@bar.com");
        when(oauthUser.getAttributes()).thenReturn(attributes);

        when(userRepository.findByEmail("new@bar.com")).thenReturn(Optional.empty());
        when(staffRepository.staffExists("new@bar.com", Role.TRAVEL_AGENT)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        SignInResponse response = service.socialLogin(oauthUser);

        assertThat(response.imageUrl()).isNull();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("new@bar.com");
        assertThat(saved.getFirstName()).isEqualTo("John");
        assertThat(saved.getLastName()).isEqualTo("Doe");
        assertThat(saved.getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void socialLogin_newUser_noGivenNameFallsBackToFullName() {
        OAuth2User oauthUser = mockOAuth2User();
        when(oauthUser.getAttribute("email")).thenReturn("new@bar.com");
        when(oauthUser.getAttribute("given_name")).thenReturn(null);
        when(oauthUser.getAttribute("name")).thenReturn("Johnny Appleseed");
        when(oauthUser.getAttribute("family_name")).thenReturn(null);
        when(oauthUser.getAttributes()).thenReturn(new HashMap<>());

        when(userRepository.findByEmail("new@bar.com")).thenReturn(Optional.empty());
        when(staffRepository.staffExists("new@bar.com", Role.TRAVEL_AGENT)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        service.socialLogin(oauthUser);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getFirstName()).isEqualTo("Johnny Appleseed");
    }

    @Test
    void socialLogin_newUser_noNamesAtAll_firstNameDefaultsToUNKNOWN() {
        OAuth2User oauthUser = mockOAuth2User();
        when(oauthUser.getAttribute("email")).thenReturn("new@bar.com");
        when(oauthUser.getAttribute("given_name")).thenReturn(null);
        when(oauthUser.getAttribute("name")).thenReturn(null);
        when(oauthUser.getAttribute("family_name")).thenReturn(null);
        when(oauthUser.getAttributes()).thenReturn(new HashMap<>());

        when(userRepository.findByEmail("new@bar.com")).thenReturn(Optional.empty());
        when(staffRepository.staffExists("new@bar.com", Role.TRAVEL_AGENT)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        service.socialLogin(oauthUser);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getFirstName()).isEqualTo("UNKNOWN");
    }

    @Test
    void socialLogin_newTravelAgent_assignsTravelAgentRole() {
        OAuth2User oauthUser = mockOAuth2User();
        when(oauthUser.getAttribute("email")).thenReturn("agent@bar.com");
        when(oauthUser.getAttribute("given_name")).thenReturn("Jane");
        when(oauthUser.getAttribute("name")).thenReturn(null);
        when(oauthUser.getAttribute("family_name")).thenReturn("Roe");
        when(oauthUser.getAttributes()).thenReturn(new HashMap<>());

        when(userRepository.findByEmail("agent@bar.com")).thenReturn(Optional.empty());
        when(staffRepository.staffExists("agent@bar.com", Role.TRAVEL_AGENT)).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        service.socialLogin(oauthUser);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.TRAVEL_AGENT);
    }

    @Test
    void socialLogin_normalizesEmailFromMixedCaseWithWhitespace() {
        OAuth2User oauthUser = mockOAuth2User();
        when(oauthUser.getAttribute("email")).thenReturn("  Foo@Bar.COM  ");
        when(oauthUser.getAttributes()).thenReturn(new HashMap<>());

        User existing = new User("user-123", "foo@bar.com", "HASH", "John", "Doe", Role.CUSTOMER);
        when(userRepository.findByEmail("foo@bar.com")).thenReturn(Optional.of(existing));
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        service.socialLogin(oauthUser);

        verify(userRepository).findByEmail("foo@bar.com");
        verify(userRepository, never()).save(any());
    }

    private static OAuth2User mockOAuth2User() {
        return org.mockito.Mockito.mock(OAuth2User.class);
    }
}
