package com.threadly.vote.config;

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

    public String getCommentBaseUrl() {
        return services.getCommentBaseUrl();
    }

    public String getAuthBaseUrl() {
        return services.getAuthBaseUrl();
    }

    public static class Services {
        private String postBaseUrl = "http://localhost:8083";
        private String commentBaseUrl = "http://localhost:8084";
        private String authBaseUrl = "http://localhost:8081";

        public String getPostBaseUrl() {
            return postBaseUrl;
        }

        public void setPostBaseUrl(String postBaseUrl) {
            this.postBaseUrl = postBaseUrl;
        }

        public String getCommentBaseUrl() {
            return commentBaseUrl;
        }

        public void setCommentBaseUrl(String commentBaseUrl) {
            this.commentBaseUrl = commentBaseUrl;
        }

        public String getAuthBaseUrl() {
            return authBaseUrl;
        }

        public void setAuthBaseUrl(String authBaseUrl) {
            this.authBaseUrl = authBaseUrl;
        }
    }
}
