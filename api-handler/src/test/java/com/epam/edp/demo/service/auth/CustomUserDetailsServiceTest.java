package com.epam.edp.demo.service.auth;

import com.epam.edp.demo.entity.auth.Role;
import com.epam.edp.demo.entity.auth.User;
import com.epam.edp.demo.repository.auth.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomUserDetailsServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final CustomUserDetailsService service = new CustomUserDetailsService(userRepository);

    @Test
    void loadUserByUsername_userExists_returnsCustomUserDetailsWithUserEmailAsUsername() {
        User user = User.builder()
                .id("user-1")
                .email("foo@bar.com")
                .password("ENCODED")
                .firstName("Foo")
                .lastName("Bar")
                .role(Role.CUSTOMER)
                .build();
        when(userRepository.findByEmail("foo@bar.com")).thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername("foo@bar.com");

        assertThat(result.getUsername()).isEqualTo("foo@bar.com");
    }

    @Test
    void loadUserByUsername_userMissing_throwsUsernameNotFoundException() {
        when(userRepository.findById("user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("user-1"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void loadUserById_userExists_returnsCustomUserDetailsWithEmailAsUsername() {
        User user = User.builder()
                .id("user-1")
                .email("foo@bar.com")
                .password("ENCODED")
                .firstName("Foo")
                .lastName("Bar")
                .role(Role.CUSTOMER)
                .build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        UserDetails result = service.loadUserById("user-1");

        assertThat(result.getUsername()).isEqualTo("foo@bar.com");
    }

    @Test
    void loadUserById_userMissing_throwsUsernameNotFoundException() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserById("missing"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");
    }
}
