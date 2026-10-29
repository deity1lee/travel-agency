package com.epam.edp.demo.entity.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "jwt-blacklist")
public class JwtBlacklist {

    @Id
    private String token;

    @Indexed(expireAfterSeconds = 0)
    private Instant expiresAt;

}
