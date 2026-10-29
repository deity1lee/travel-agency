package com.epam.edp.demo.service.auth;

import com.epam.edp.demo.controller.UserContext;
import com.epam.edp.demo.dto.auth.*;
import com.epam.edp.demo.dto.MessageResponse;
import com.epam.edp.demo.dto.document.EditProfileResponse;
import com.epam.edp.demo.entity.auth.*;
import com.epam.edp.demo.exception.UserNotFoundException;
import com.epam.edp.demo.exception.auth.*;
import com.epam.edp.demo.mapper.UserMapper;
import com.epam.edp.demo.repository.auth.JwtBlacklistRepository;
import com.epam.edp.demo.repository.auth.UserRepository;
import com.epam.edp.demo.repository.staff.StaffRepository;
import com.epam.edp.demo.service.document.S3PresignedUrlService;
import com.epam.edp.demo.service.captcha.CaptchaService;
import com.epam.edp.demo.service.email.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final JwtBlacklistRepository jwtBlacklistRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final VerificationTokenService verificationTokenService;
    private final EmailService emailService;
    private final CaptchaService captchaService;
    private final S3PresignedUrlService presignedUrlService;

    public AuthServiceImpl(UserRepository userRepository,
                           StaffRepository staffRepository,
                           JwtBlacklistRepository jwtBlacklistRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtService jwtService,
                           VerificationTokenService verificationTokenService,
                           EmailService emailService,
                           CaptchaService captchaService,
                           S3PresignedUrlService presignedUrlService) {
        this.userRepository = userRepository;
        this.staffRepository = staffRepository;
        this.jwtBlacklistRepository = jwtBlacklistRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.verificationTokenService = verificationTokenService;
        this.emailService = emailService;
        this.captchaService = captchaService;
        this.presignedUrlService = presignedUrlService;
    }

    @Override
    public MessageResponse signUp(SignUpRequest request) {
        captchaService.validate(request.captchaToken());
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new EmailAlreadyExistsException("User with this email already exists");
        }

        String hashedPassword = passwordEncoder.encode(request.password());
        String code = verificationTokenService.createSignupToken(
                request.firstName(), request.lastName(), normalizedEmail, hashedPassword);

        emailService.sendSignupVerificationEmail(normalizedEmail, code);
        return new MessageResponse("Verification code sent");
    }

    @Override
    public MessageResponse verifySignUp(VerifySignUpRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        log.info("Verifying sign-up code for email: {}", normalizedEmail);

        EmailVerificationToken token = (EmailVerificationToken) verificationTokenService.verifyCode(
                normalizedEmail, request.code(), VerificationType.SIGNUP);

        Role role = findRole(normalizedEmail);
        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .email(normalizedEmail)
                .firstName(token.getFirstName())
                .lastName(token.getLastName())
                .password(token.getPassword())
                .role(role)
                .build();

        userRepository.save(user);
        verificationTokenService.delete(token);

        log.info("Account created for email: {} with role {}", normalizedEmail, role);
        return new MessageResponse("Account created successfully");
    }

    @Override
    public SignInResponse signIn(SignInRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        boolean notVerified = verificationTokenService.existsByEmailAndType(normalizedEmail, VerificationType.SIGNUP);
        if (notVerified) {
            throw new EmailNotVerifiedException("Please verify your email before signing in.");
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            normalizedEmail,
                            request.password()
                    )
            );
        } catch (AuthenticationException e) {
            log.debug("Sign-in attempt with invalid email or password", e);
            throw new BadCredentialsException("Invalid email or password");
        }
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(user);

        String s3Key = String.format(S3PresignedUrlService.PICTURE_UPLOAD_URL_BASE, user.getId());
        String uploadUrl = null;
        String bucketName = presignedUrlService.getDocumentsBucketName();
        if (presignedUrlService.verifyFileExistsByKey(bucketName, s3Key)) {
            uploadUrl = presignedUrlService.generateRetrieveUrl(bucketName, s3Key);
        }

        return UserMapper.toSignInResponse(user, token, uploadUrl);
    }

    @Override
    public LogoutResponse logout(String token) {
        if (token == null) {
            throw new BadCredentialsException("Missing or invalid Authorization token");
        }

        JwtBlacklist entity = new JwtBlacklist();
        entity.setToken(token);
        entity.setExpiresAt(jwtService.extractExpiration(token).toInstant());
        jwtBlacklistRepository.save(entity);
        return new LogoutResponse("Logged out successfully");
    }

    @Override
    public EditProfileResponse editNames(UserContext userContext, EditUserNamesRequest request) {
        log.info("Editing first/last names for user {}", userContext.email());
        String uploadUrl = null;
        if (request.photo() != null) {
            log.info("Constructing picture upload url for user: {}", userContext.userId());
            String s3Key = String.format(S3PresignedUrlService.PICTURE_UPLOAD_URL_BASE, userContext.userId());
            String bucketName = presignedUrlService.getDocumentsBucketName();
            uploadUrl = presignedUrlService.generatePictureUploadUrl(bucketName, s3Key, request.photo().fileFormat());
        }
        userRepository.updateNames(userContext.userId(), request.firstName(), request.lastName());
        log.info("Edited names for user {}", userContext.email());
        return new EditProfileResponse("Profile edited successfully", uploadUrl);
    }

    @Override
    public MessageResponse editEmail(UserContext userContext, EditEmailRequest request) {
        log.info("Editing email for user {}", userContext.email());
        User user = userRepository.findById(userContext.userId())
                .orElseThrow(() -> new UserNotFoundException(String.format("User with id %s not found", userContext.userId())));

        if (!userContext.email().equals(user.getEmail())) {
            log.debug("User {} has changed email to {} in this session", user.getId(), user.getEmail());
        }

        if (userRepository.existsByEmail(request.newEmail())) {
            throw new EmailAlreadyExistsException("User with this email already exists");
        }

        log.info("Checking if password hashes match for user {}", user.getEmail());
        if (principalPasswordMatches(request.password())) {
            throw new IncorrectPasswordException("Invalid password");
        }

        String token = verificationTokenService.createEmailChangeToken(user.getEmail(), request.newEmail());

        log.info("Sending confirmation email to user {}", request.newEmail());
        emailService.sendChangeEmailEmail(request.newEmail(), token);

        return new MessageResponse("Email verification sent");
    }

    @Override
    public MessageResponse editPassword(UserContext userContext, EditPasswordRequest request) {
        log.info("Editing password for user {}", userContext.email());

        if (principalPasswordMatches(request.oldPassword())) {
            throw new IncorrectPasswordException("Invalid password");
        }

        String newPassword = passwordEncoder.encode(request.newPassword());
        userRepository.updatePassword(userContext.userId(), newPassword);
        return new MessageResponse("Password edited successfully");
    }

    @Override
    public MessageResponse sendVerificationCodeForPasswordReset(SendVerificationCodeRequest email) {
        String normalizedEmail = email.email().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException(String.format("User with email %s not found", normalizedEmail)));

        if (user.getPassword() == null) {
            log.warn("User is logged in with Google account, cannot reset password: {}", normalizedEmail);
            throw new EditProfileException("User is logged in with Google account, cannot change password");
        }
        log.info("creating verification token for email {}", normalizedEmail);
        String token = verificationTokenService.createPasswordResetToken(email.email());
        emailService.sendPasswordResetVerificationEmail(normalizedEmail, token);
        return new MessageResponse("Verification code was sent");
    }

    @Override
    public void verifyEmailChangeToken(UserContext userContext, AuthenticatedOptVerifyRequest request) {
        log.info("Verifying email change token for user {}", userContext.email());
        EmailChangeToken tok = (EmailChangeToken) verificationTokenService.verifyCode(
                userContext.email(), request.code(), VerificationType.CHANGE_EMAIL);

        log.info("Token verified, updating email for user {}", userContext.email());
        userRepository.updateEmail(userContext.userId(), tok.getNewEmail());
        verificationTokenService.delete(tok);
    }

    @Override
    public void verifyPasswordResetToken(String email, String token) {
        verificationTokenService.verifyCode(email, token, VerificationType.PASSWORD_RESET);
    }

    @Override
    public MessageResponse resendOtpAuth(ResendCodeRequest request) {
        log.info("Requesting new OPT for email {}", request.email());
        String code = verificationTokenService.getNewOpt(request.email(), request.verificationType());
        log.info("Sending new OTP to email {}", request.email());
        if (request.verificationType() == VerificationType.CHANGE_EMAIL) {
            emailService.sendChangeEmailEmail(request.email(), code);
            return new MessageResponse("Verification code sent");
        }
        throw new BadCredentialsException("Unsupported verification type: " + request.verificationType()); //exception?
    }

    @Override
    public MessageResponse resendOtpNotAuth(ResendCodeRequest request) {
        log.info("Requesting new OPT for email {}", request.email());
        String code = verificationTokenService.getNewOpt(request.email(), request.verificationType());
        log.info("Sending new OTP to email {}", request.email());
        if (request.verificationType() == VerificationType.SIGNUP) {
            emailService.sendSignupVerificationEmail(request.email(), code);
            return new MessageResponse("Verification code sent");
        }
        if (request.verificationType() == VerificationType.PASSWORD_RESET) {
            emailService.sendPasswordResetVerificationEmail(request.email(), code);
            return new MessageResponse("Verification code sent");
        }
        throw new BadCredentialsException("Unsupported verification type: " + request.verificationType()); //exception?
    }

    @Override
    public MessageResponse resetPassword(String email, String s) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new VerificationFailedException("User not found"));
        PasswordResetToken tok = verificationTokenService.verifyPasswordResetTokenWasValidated(email, VerificationType.PASSWORD_RESET);
        String newPassword = passwordEncoder.encode(s);
        userRepository.updatePassword(user.getId(), newPassword);
        verificationTokenService.delete(tok);
        return new MessageResponse("Password edited successfully");
    }

    @Override
    public Role findRole(String email) {
        log.info("Looking up role for user {}", email);
        Role role = Role.CUSTOMER;

        if (staffRepository.staffExists(email, Role.TRAVEL_AGENT)) {
            role = Role.TRAVEL_AGENT;
        } else if (staffRepository.staffExists(email, Role.ADMIN)) {
            role = Role.ADMIN;
        }

        log.info("Found role {} for user {}", role, email);
        return role;
    }

    private boolean principalPasswordMatches(String password) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails principal = (UserDetails) authentication.getPrincipal();
        log.debug("fetched principal for {}", principal.getUsername());

        String passwordHash = principal.getPassword();
        if (passwordHash == null || passwordHash.isEmpty()) {
            log.warn("User is logged in with Google account, cannot change password: {}", principal.getUsername());
            throw new EditProfileException("User cannot change her/his password");
        }

        log.info("Checking if password hashes match");
        return !passwordEncoder.matches(password, passwordHash);
    }
}
