package com.epam.edp.demo.service.auth;

import com.epam.edp.demo.controller.UserContext;
import com.epam.edp.demo.dto.auth.*;
import com.epam.edp.demo.dto.MessageResponse;
import com.epam.edp.demo.dto.document.EditProfileResponse;
import com.epam.edp.demo.entity.auth.Role;

public interface AuthService {
    MessageResponse signUp(SignUpRequest request);

    MessageResponse verifySignUp(VerifySignUpRequest request);

    SignInResponse signIn(SignInRequest request);

    LogoutResponse logout(String token);

    Role findRole(String email);

    EditProfileResponse editNames(UserContext userContext, EditUserNamesRequest request);

    MessageResponse editEmail(UserContext userContext, EditEmailRequest request);

    MessageResponse editPassword(UserContext userContext, EditPasswordRequest request);

    MessageResponse resetPassword(String email, String s);

    MessageResponse sendVerificationCodeForPasswordReset(SendVerificationCodeRequest email);

    void verifyEmailChangeToken(UserContext email, AuthenticatedOptVerifyRequest request);

    void verifyPasswordResetToken(String email, String token);

    MessageResponse resendOtpAuth(ResendCodeRequest request);

    MessageResponse resendOtpNotAuth(ResendCodeRequest request);
}
