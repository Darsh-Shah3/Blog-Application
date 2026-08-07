package com.threadly.post.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class RemoteServiceProperties {

    private String internalApiKey;
    private final Services services = new Services();

    public String getInternalApiKey() {
        return internalApiKey;
    }

    public void setInternalApiKey(String internalApiKey) {
        this.internalApiKey = internalApiKey;
    }

    public Services getServices() {
        return services;
    }

    public String getCommunityBaseUrl() {
        return services.getCommunityBaseUrl();
    }

    public String getAuthBaseUrl() {
        return services.getAuthBaseUrl();
    }

    public String getAuditBaseUrl() {
        return services.getAuditBaseUrl();
    }

    public String getNotificationBaseUrl() {
        return services.getNotificationBaseUrl();
    }

    public static class Services {
        private String communityBaseUrl = "http://localhost:8082";
        private String authBaseUrl = "http://localhost:8081";
        private String auditBaseUrl = "http://localhost:8087";
        private String notificationBaseUrl = "http://localhost:8088";

        public String getCommunityBaseUrl() {
            return communityBaseUrl;
        }

        public void setCommunityBaseUrl(String communityBaseUrl) {
            this.communityBaseUrl = communityBaseUrl;
        }

        public String getAuthBaseUrl() {
            return authBaseUrl;
        }

        public void setAuthBaseUrl(String authBaseUrl) {
            this.authBaseUrl = authBaseUrl;
        }

        public String getAuditBaseUrl() {
            return auditBaseUrl;
        }

        public void setAuditBaseUrl(String auditBaseUrl) {
            this.auditBaseUrl = auditBaseUrl;
        }

        public String getNotificationBaseUrl() {
            return notificationBaseUrl;
        }

        public void setNotificationBaseUrl(String notificationBaseUrl) {
            this.notificationBaseUrl = notificationBaseUrl;
        }
    }
}
