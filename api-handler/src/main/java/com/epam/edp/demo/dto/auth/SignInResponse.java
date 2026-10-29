package com.epam.edp.demo.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SignInResponse(
        String token,
        String role,
        String userName,
        String email,
        String imageUrl
) {
}
