package com.epam.edp.demo.dto.document;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EditProfileResponse(
        String message,
        String uploadUrl
) {
}
