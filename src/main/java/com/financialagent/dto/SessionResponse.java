package com.financialagent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from AngelOne SmartAPI login endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionResponse {

    private boolean status;
    private String message;
    private String errorcode;
    private SessionData data;

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorcode() {
        return errorcode;
    }

    public void setErrorcode(String errorcode) {
        this.errorcode = errorcode;
    }

    public SessionData getData() {
        return data;
    }

    public void setData(SessionData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SessionData {
        @JsonProperty("jwtToken")
        private String jwtToken;

        @JsonProperty("refreshToken")
        private String refreshToken;

        @JsonProperty("feedToken")
        private String feedToken;

        public String getJwtToken() {
            return jwtToken;
        }

        public void setJwtToken(String jwtToken) {
            this.jwtToken = jwtToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public String getFeedToken() {
            return feedToken;
        }

        public void setFeedToken(String feedToken) {
            this.feedToken = feedToken;
        }
    }

    /**
     * Login request body for AngelOne SmartAPI.
     */
    public static class LoginRequest {
        private String clientcode;
        private String password;
        private String totp;

        public LoginRequest() {
        }

        public LoginRequest(String clientcode, String password, String totp) {
            this.clientcode = clientcode;
            this.password = password;
            this.totp = totp;
        }

        public String getClientcode() {
            return clientcode;
        }

        public void setClientcode(String clientcode) {
            this.clientcode = clientcode;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getTotp() {
            return totp;
        }

        public void setTotp(String totp) {
            this.totp = totp;
        }
    }
}
