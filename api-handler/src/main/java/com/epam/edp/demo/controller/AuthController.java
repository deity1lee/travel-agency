package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.auth.*;
import com.epam.edp.demo.dto.MessageResponse;
import com.epam.edp.demo.dto.document.EditProfileResponse;
import com.epam.edp.demo.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Endpoints for user authentication and account management")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/sign-up")
    @Operation(
            summary = "Sign up",
            description = "Step 1 of verified registration: stashes the registration and emails a 6-digit code. No account is created until the code is verified."
    )
    @ApiResponse(responseCode = "200", description = "Verification code sent")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "409", description = "User with this email already exists")
    @ApiResponse(responseCode = "503", description = "Unable to send verification email")
    public MessageResponse signUp(@Valid @RequestBody SignUpRequest request) {
        log.info("Received sign-up request for email: {}", request.email());
        MessageResponse response = authService.signUp(request);
        log.info("Verification code sent for sign-up email: {}", request.email());
        return response;
    }

    @PostMapping("/sign-up/verify")
    @Operation(
            summary = "Verify sign-up",
            description = "Step 2 of verified registration: submit the 6-digit code from the email. On a valid, unexpired code the account is created. No JWT is issued; sign in separately."
    )
    @ApiResponse(responseCode = "201", description = "Account created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid or expired verification code, or invalid request data")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse verifySignUp(@Valid @RequestBody VerifySignUpRequest request) {
        log.info("Received sign-up verification request for email: {}", request.email());
        MessageResponse response = authService.verifySignUp(request);
        log.info("Sign-up verified and account created for email: {}", request.email());
        return response;
    }

    @PostMapping("/non/resend")
    @Operation(
            summary = "Resend for not auth endpoints",
            description = "Request to resend verification code to provided email if it exists or previous token is not expired"
    )
    @ApiResponse(responseCode = "200", description = "Verification code sent")
    @ApiResponse(responseCode = "400", description = "Invalid request data, or invalid/expired verification code")
    @ApiResponse(responseCode = "503", description = "Unable to send verification email")
    public MessageResponse resendOtpNotAuth(@Valid @RequestBody ResendCodeRequest request) {
        log.info("Incoming request: POST /auth/non/resend, user={}, type={}", request.email(), request.verificationType());
        MessageResponse response = authService.resendOtpNotAuth(request);
        log.info("Response: 200 OK – resent code to user email {}", request.email());
        return response;
    }

    @PostMapping("/resend")
    @Operation(
            summary = "Resend for auth endpoints",
            description = "Request to resend verification code to provided email if it exists or previous token is not expired"
    )
    @ApiResponse(responseCode = "200", description = "Verification code sent")
    @ApiResponse(responseCode = "400", description = "Invalid request data, or invalid/expired verification code")
    @ApiResponse(responseCode = "503", description = "Unable to send verification email")
    public MessageResponse resendOtpAuth(@Valid @RequestBody ResendCodeRequest request) {
        log.info("Incoming request: POST /auth/resend, user={}", request.email());
        MessageResponse response = authService.resendOtpAuth(request);
        log.info("Response: 200 OK – resent code to user email {}", request.email());
        return response;
    }

    @PostMapping("/sign-in")
    @Operation(
            summary = "Sign in",
            description = "Sign in with email and password."
    )
    @ApiResponse(responseCode = "200", description = "User details")
    @ApiResponse(responseCode = "400", description = "Auth validation error: email: Invalid email address.")
    @ApiResponse(responseCode = "403", description = "Email not verified")
    @ApiResponse(responseCode = "401", description = "Invalid email or password")
    public SignInResponse signIn(@Valid @RequestBody SignInRequest request) {
        log.info("Received sign-in request for email: {}", request.email());
        SignInResponse response = authService.signIn(request);
        log.info("User with email: {} signed in successfully", request.email());
        return response;
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Log out",
            description = "Log out from the account."
    )
    @ApiResponse(responseCode = "200", description = "Logged out successfully")
    @ApiResponse(responseCode = "401", description = "Invalid or expired token.")
    public LogoutResponse logout(@Parameter(hidden = true)
                                 @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.info("Authorization header received: {}", authorization);
        String token = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7) : authorization;
        return authService.logout(token);
    }

    @PatchMapping("/me")
    @Operation(
            summary = "Edit user profile",
            description = "Edit user profile, including first name, last name, and adding photoUpload"
    )
    @ApiResponse(responseCode = "200", description = "Account edited successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    public EditProfileResponse editNames(@CurrentUser UserContext userContext,
                                         @Valid @RequestBody EditUserNamesRequest request) {
        log.info("Incoming request: PATCH /me, user={}", userContext.email());
        EditProfileResponse response = authService.editNames(userContext, request);
        log.info("Response: 200 OK – User {} profile was edited successfully", userContext.email());
        return response;
    }

    @PostMapping("/me/email")
    @Operation(
            summary = "Edit user profile email",
            description = "Edit user email"
    )
    @ApiResponse(responseCode = "200", description = "Email changed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "403", description = "Invalid password")
    @ApiResponse(responseCode = "409", description = "User with this email already exists")
    @ApiResponse(responseCode = "503", description = "Unable to send verification email")
    public MessageResponse editEmail(@CurrentUser UserContext userContext,
                                     @Valid @RequestBody EditEmailRequest request) {
        log.info("Incoming request: POST /me/email, user={}", userContext.email());
        MessageResponse response = authService.editEmail(userContext, request);
        log.info("Response: 200 OK – Confirmation email sent to {}", request.newEmail());
        return response;
    }

    @PostMapping("/me/password")
    @Operation(
            summary = "Edit user profile password",
            description = "Edit user password"
    )
    @ApiResponse(responseCode = "200", description = "Password changed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "403", description = "Invalid password")
    @ApiResponse(responseCode = "409", description = "User cannot change her/his password, action not allowed")
    public MessageResponse editPassword(@CurrentUser UserContext userContext,
                                        @Valid @RequestBody EditPasswordRequest request) {
        log.info("Incoming request: POST /me/password, user={}", userContext.email());
        MessageResponse response = authService.editPassword(userContext, request);
        log.info("Response: 200 OK – User {} password was edited successfully", userContext.email());
        return response;
    }

    @PostMapping("/email/verify")
    @Operation(
            summary = "Verify code, change email",
            description = "Verify the code sent via email and change user email"
    )
    @ApiResponse(responseCode = "200", description = "Code verified and email changed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid or expired verification code, or invalid request data")
    public MessageResponse verifyTokenAndChangeEmail(@CurrentUser UserContext userContext,
                                                     @RequestBody AuthenticatedOptVerifyRequest request) {
        log.info("Incoming request: POST /email/verify, user={}", userContext.email());
        authService.verifyEmailChangeToken(userContext, request);
        log.info("Response: 200 OK – code sent to new user email");
        return new MessageResponse("Email has been successfully verified");
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Send password recovery verification code",
            description = "Sends a verification code to the user's email address to start the password recovery process."
    )
    @ApiResponse(responseCode = "200", description = "Verification code was sent successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "401", description = "No user found with the provided email address")
    @ApiResponse(responseCode = "409", description = "User cannot change her/his password, action not allowed")
    public MessageResponse sendVerificationCodeForPasswordReset(@Valid @RequestBody SendVerificationCodeRequest email) {
        log.info("Received send verification code request for email: {}", email.email());
        MessageResponse response = authService.sendVerificationCodeForPasswordReset(email);
        log.info("Verification code was sent to {}", email.email());
        return response;
    }
    @PostMapping("/forgot-password/verify")
    @Operation(
            summary = "Verify password recovery code",
            description = "Validates the verification code sent to the user's email before allowing password reset."
    )
    @ApiResponse(responseCode = "200", description = "Verification code was successfully validated")
    @ApiResponse(responseCode = "400", description = "Invalid request data | The provided verification code is incorrect or expired")
    public MessageResponse verifyPasswordResetCode(@Valid @RequestBody VerifyResetPasswordCodeRequest request) {
        log.info("Received verification code request for email: {}", request.email());
        authService.verifyPasswordResetToken(request.email(), request.code());
        return new MessageResponse("verified");
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Reset user password",
            description = "Updates the user's password after successful verification of the password recovery request."
    )
    @ApiResponse(responseCode = "200", description = "Password was reset successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Received new password for email: {}", request.email());
        MessageResponse response = authService.resetPassword(request.email(), request.newPassword());
        log.info("Password was reset successfully for email: {}", request.email());
        return response;
    }
}
