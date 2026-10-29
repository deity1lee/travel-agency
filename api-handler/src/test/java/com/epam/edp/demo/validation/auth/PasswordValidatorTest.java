package com.epam.edp.demo.validation.auth;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordValidatorTest {

    private PasswordValidator validator;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new PasswordValidator();
        context = mock(ConstraintValidatorContext.class);
        ConstraintViolationBuilder builder = mock(ConstraintViolationBuilder.class);
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
    }

    @Test
    void isValid_nullPassword_returnsTrueAndDoesNotAddViolation() {
        assertThat(validator.isValid(null, context)).isTrue();
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    void isValid_validPassword_returnsTrueAndAddsNoViolations() {
        assertThat(validator.isValid("Abcdef1!", context)).isTrue();
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"A1!a", "Abc1!", "Aa1!aaa"})
    void isValid_tooShort_returnsFalseWithLengthViolation(String pwd) {
        assertThat(validator.isValid(pwd, context)).isFalse();
        assertThat(capturedMessages()).contains("Password must be 8-16 characters long");
    }

    @Test
    void isValid_tooLong_returnsFalseWithLengthViolation() {
        String tooLong = "Abcdef1!Abcdef1!X";
        assertThat(validator.isValid(tooLong, context)).isFalse();
        assertThat(capturedMessages()).contains("Password must be 8-16 characters long");
    }

    @Test
    void isValid_missingUppercase_returnsFalseWithUppercaseViolation() {
        assertThat(validator.isValid("abcdef1!", context)).isFalse();
        assertThat(capturedMessages()).contains("At least one uppercase letter required");
    }

    @Test
    void isValid_missingLowercase_returnsFalseWithLowercaseViolation() {
        assertThat(validator.isValid("ABCDEF1!", context)).isFalse();
        assertThat(capturedMessages()).contains("At least one lowercase letter required");
    }

    @Test
    void isValid_missingDigit_returnsFalseWithDigitViolation() {
        assertThat(validator.isValid("Abcdefg!", context)).isFalse();
        assertThat(capturedMessages()).contains("At least one number required");
    }

    @Test
    void isValid_missingSpecialCharacter_returnsFalseWithSpecialViolation() {
        assertThat(validator.isValid("Abcdef12", context)).isFalse();
        assertThat(capturedMessages()).contains("At least one special character required");
    }

    @Test
    void isValid_allRulesFail_collectsAllViolations() {
        assertThat(validator.isValid("aaa", context)).isFalse();
        List<String> messages = capturedMessages();
        assertThat(messages).contains(
                "Password must be 8-16 characters long",
                "At least one uppercase letter required",
                "At least one number required",
                "At least one special character required");
    }

    @Test
    void isValid_disablesDefaultConstraintViolation() {
        validator.isValid("abc", context);
        verify(context).disableDefaultConstraintViolation();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Abcdef1!", "Abcdef1@", "Abcdef1#", "Abcdef1$", "Abcdef1%", "Abcdef1^",
            "Abcdef1&", "Abcdef1*", "Abcdef1(", "Abcdef1)", "Abcdef1_", "Abcdef1+",
            "Abcdef1-", "Abcdef1[", "Abcdef1]", "Abcdef1{", "Abcdef1}", "Abcdef1;",
            "Abcdef1'", "Abcdef1:", "Abcdef1\"", "Abcdef1\\", "Abcdef1|", "Abcdef1,",
            "Abcdef1.", "Abcdef1<", "Abcdef1>", "Abcdef1/", "Abcdef1?"})
    void isValid_acceptsAllRecognizedSpecialCharacters(String pwd) {
        assertThat(validator.isValid(pwd, context)).isTrue();
    }

    private List<String> capturedMessages() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(context, atLeastOnce()).buildConstraintViolationWithTemplate(captor.capture());
        return captor.getAllValues();
    }
}
