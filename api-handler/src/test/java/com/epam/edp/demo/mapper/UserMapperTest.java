package com.epam.edp.demo.mapper;

import com.epam.edp.demo.dto.auth.SignInResponse;
import com.epam.edp.demo.entity.auth.CustomUserDetails;
import com.epam.edp.demo.entity.auth.Role;
import com.epam.edp.demo.entity.auth.User;
import com.epam.edp.demo.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserMapperTest {

    @Test
    void toSignInResponse_populatesAllFieldsFromUserDetailsAndToken() {
        String firstName = "John";
        String lastName = "Doe";
        CustomUserDetails userDetails = new CustomUserDetails(
                new User(
                        "user-123",
                        "foo@bar.com",
                        "ENCODED",
                        firstName,
                        lastName,
                        Role.CUSTOMER
                )
        );

        SignInResponse response = UserMapper.toSignInResponse(userDetails, "jwt-token", null);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.role()).isEqualTo(Role.CUSTOMER.name());
        assertThat(response.userName()).isEqualTo(firstName + " " + lastName);
        assertThat(response.email()).isEqualTo("foo@bar.com");
    }

    @Test
    void toSignInResponse_throwsUserNotFoundExceptionWhenAuthoritiesMissing() {
        // CustomUserDetails always derives a single authority from the user's role, so the
        // no-authority branch is only reachable with a stubbed UserDetails carrying none.
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUsername()).thenReturn("foo@bar.com");
        doReturn(List.<GrantedAuthority>of()).when(userDetails).getAuthorities();

        assertThatThrownBy(() -> UserMapper.toSignInResponse(userDetails, "jwt-token", null))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("foo@bar.com");
    }
}
