package com.threadly.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public class MailAppProperties {

    /** When false, skip SMTP and log the reset link (local/dev). */
    private boolean enabled = false;
    private String from = "noreply@threadly.local";
    /** Public frontend base used to build reset links, e.g. http://localhost:3000 */
    private String frontendBaseUrl = "http://localhost:3000";
    /** Token lifetime in minutes */
    private long resetTokenMinutes = 30;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getFrontendBaseUrl() {
        return frontendBaseUrl;
    }

    public void setFrontendBaseUrl(String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public long getResetTokenMinutes() {
        return resetTokenMinutes;
    }

    public void setResetTokenMinutes(long resetTokenMinutes) {
        this.resetTokenMinutes = resetTokenMinutes;
    }
}
