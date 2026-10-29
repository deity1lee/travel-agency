package com.epam.edp.demo.entity.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationToken extends VerificationToken {

    private String firstName;
    private String lastName;
    private String password;
}
