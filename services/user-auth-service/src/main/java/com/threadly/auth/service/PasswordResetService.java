package com.threadly.auth.service;

import com.threadly.auth.dto.ForgotPasswordRequest;
import com.threadly.auth.dto.MessageResponse;
import com.threadly.auth.dto.ResetPasswordRequest;

public interface PasswordResetService {

    MessageResponse forgotPassword(ForgotPasswordRequest request);

    MessageResponse resetPassword(ResetPasswordRequest request);
}
