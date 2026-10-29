package com.epam.edp.demo.dto.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaptchaVerifyResponse {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("score")
    private double score;

    @JsonProperty("action")
    private String action;

    @JsonProperty("error-codes")
    private List<String> errorCodes;
}
