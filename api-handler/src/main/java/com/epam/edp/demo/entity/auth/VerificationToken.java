package com.epam.edp.demo.entity.auth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@Document(collection = "tokens")
public abstract class VerificationToken {

    @Id
    protected String id;

    protected String email; // pk
    protected VerificationType type; //SK": "EMAIL_VERIFY"         #OTP",
    protected String tokenHash; // "code": "483921",

    @Indexed(expireAfterSeconds = 0)
    protected LocalDateTime expiresAt;
    protected int attempts = 0;

    public String getEmail() {
        return email;
    }

    public VerificationType getType() {
        return type;
    }
}