package com.threadly.auth.service;

import com.threadly.auth.config.MailAppProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends password-reset email via SMTP when configured; otherwise logs the link for local dev.
 */
@Service
@RequiredArgsConstructor
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final MailAppProperties mailProps;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    public void sendPasswordReset(String toEmail, String resetUrl) {
        if (!mailProps.isEnabled()) {
            log.warn("SMTP disabled (MAIL_ENABLED=false) — reset link for {} → {}", toEmail, resetUrl);
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException("MAIL_ENABLED=true but spring.mail.host is not configured");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProps.getFrom());
        message.setTo(toEmail);
        message.setSubject("Threadly — reset your password");
        message.setText(
                "You requested a password reset for Threadly.\n\n"
                        + "Open this link within "
                        + mailProps.getResetTokenMinutes()
                        + " minutes:\n"
                        + resetUrl
                        + "\n\nIf you did not request this, ignore this email.");
        mailSender.send(message);
        log.info("Password reset email sent to={}", toEmail);
    }
}
