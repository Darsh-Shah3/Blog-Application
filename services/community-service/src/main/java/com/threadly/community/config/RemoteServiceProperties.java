package com.threadly.community.config;

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

    public String getNotificationBaseUrl() {
        return services.getNotificationBaseUrl();
    }

    public static class Services {
        private String notificationBaseUrl = "http://localhost:8088";

        public String getNotificationBaseUrl() {
            return notificationBaseUrl;
        }

        public void setNotificationBaseUrl(String notificationBaseUrl) {
            this.notificationBaseUrl = notificationBaseUrl;
        }
    }
}
