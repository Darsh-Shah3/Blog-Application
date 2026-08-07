package com.threadly.comment.config;

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

    public String getPostBaseUrl() {
        return services.getPostBaseUrl();
    }

    public String getAuthBaseUrl() {
        return services.getAuthBaseUrl();
    }

    public static class Services {
        private String postBaseUrl = "http://localhost:8083";
        private String authBaseUrl = "http://localhost:8081";

        public String getPostBaseUrl() {
            return postBaseUrl;
        }

        public void setPostBaseUrl(String postBaseUrl) {
            this.postBaseUrl = postBaseUrl;
        }

        public String getAuthBaseUrl() {
            return authBaseUrl;
        }

        public void setAuthBaseUrl(String authBaseUrl) {
            this.authBaseUrl = authBaseUrl;
        }
    }
}
