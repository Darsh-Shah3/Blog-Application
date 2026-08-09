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
 * Sends transactional email via SMTP when configured; otherwise logs for local dev.
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
        JavaMailSender mailSender = requireSender();
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

    /**
     * Notifies a member that a platform admin soft-deleted their account.
     * Posts and other content may remain; login is disabled.
     */
    public void sendAccountDeletedByAdmin(String toEmail, String username) {
        String name = username == null || username.isBlank() ? "member" : username.trim();
        if (!mailProps.isEnabled()) {
            log.warn("SMTP disabled — would notify account deletion for @{} → {}", name, toEmail);
            return;
        }
        try {
            JavaMailSender mailSender = requireSender();
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailProps.getFrom());
            message.setTo(toEmail);
            message.setSubject("Threadly — your account has been deleted");
            message.setText(
                    "Hello @" + name + ",\n\n"
                            + "Your Threadly account has been deleted by a platform administrator.\n\n"
                            + "You can no longer sign in with this account. Posts and messages you already "
                            + "created may still appear for others, and will show as from a deleted account.\n\n"
                            + "If you believe this was a mistake, contact the Threadly support team.\n\n"
                            + "— Threadly");
            mailSender.send(message);
            log.info("Account-deleted email sent to={} username={}", toEmail, name);
        } catch (Exception e) {
            // Never fail the delete action solely because SMTP is down
            log.warn("Failed to send account-deleted email to={}: {}", toEmail, e.getMessage());
        }
    }

    private JavaMailSender requireSender() {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException("MAIL_ENABLED=true but spring.mail.host is not configured");
        }
        return mailSender;
    }
}
