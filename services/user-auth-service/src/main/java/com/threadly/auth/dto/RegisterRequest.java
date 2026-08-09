package com.threadly.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    @Size(max = 100)
    private String displayName;

    /**
     * Google reCAPTCHA v2 response token from the client widget.
     * Required when captcha is configured on the server; ignored when disabled.
     */
    private String captchaToken;
}
