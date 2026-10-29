package com.epam.edp.demo.entity.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PasswordResetToken extends VerificationToken {
    private boolean isVerified;
}
