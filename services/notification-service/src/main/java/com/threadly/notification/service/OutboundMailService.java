package com.threadly.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Optional email alerts when someone you follow posts.
 * Disabled when MAIL_ENABLED=false (logs instead).
 */
@Service
public class OutboundMailService {

    private static final Logger log = LoggerFactory.getLogger(OutboundMailService.class);

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;
    private final String frontendBaseUrl;

    public OutboundMailService(
            ObjectProvider<JavaMailSender> mailSender,
            @Value("${app.mail.enabled:false}") boolean enabled,
            @Value("${app.mail.from:noreply@threadly.local}") String from,
            @Value("${app.frontend-base-url:http://localhost:3000}") String frontendBaseUrl) {
        this.mailSender = mailSender.getIfAvailable();
        this.enabled = enabled;
        this.from = from;
        this.frontendBaseUrl = frontendBaseUrl.endsWith("/")
                ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
                : frontendBaseUrl;
    }

    public void sendNewPostAlert(String toEmail, String authorUsername, String postTitle, String relativeLink) {
        if (toEmail == null || toEmail.isBlank()) {
            return;
        }
        String url = frontendBaseUrl + relativeLink;
        String subject = "Threadly — @" + authorUsername + " posted something new";
        String text = "Someone you follow just published a post.\n\n"
                + "Author: @" + authorUsername + "\n"
                + "Title: " + postTitle + "\n"
                + "Open: " + url + "\n";

        if (!enabled || mailSender == null) {
            log.info("MAIL disabled — follow post alert to={} author={} link={}", toEmail, authorUsername, url);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(toEmail);
            msg.setSubject(subject);
            msg.setText(text);
            mailSender.send(msg);
            log.info("Follow post email sent to={} author={}", toEmail, authorUsername);
        } catch (Exception e) {
            log.warn("Follow post email failed to={}: {}", toEmail, e.getMessage());
        }
    }
}
