package com.financialagent.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for AngelOne SmartAPI.
 * Now only contains base URL since credentials are stored in database.
 */
@Data
@ConfigurationProperties(prefix = "angelone")
public class AngelOneConfig {

    @NotBlank(message = "AngelOne base URL is required")
    private String baseUrl = "https://apiconnect.angelbroking.com";

    private boolean totpLoginEnabled = false;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Always returns false since legacy environment variables are not supported.
     *
     * @return false
     */
    public boolean isValid() {
        return false; // Legacy credentials not supported
    }

    /**
     * Always returns false since UI login is now handled by database authentication.
     *
     * @return false
     */
    public boolean isValidForUiLogin() {
        return false; // Database authentication required
    }

    /**
     * Returns error message for legacy configuration.
     */
    public String getMissingCredentials() {
        return "Legacy environment variables are not supported. Please use database authentication by registering an account.";
    }

    /**
     * Always returns false since UI login is not supported.
     *
     * @return false
     */
    public boolean isEnableUiLogin() {
        return false; // UI login not supported
    }

    /**
     * No-op method for compatibility.
     */
    public void setUiCredentials(String clientId, String password, String totpSecret) {
        // No-op - credentials stored in database
    }

    /**
     * No-op method for compatibility.
     */
    public void clearUiCredentials() {
        // No-op - credentials stored in database
    }

    // Missing AngelOne credential getters as workaround for Lombok annotation processing issue
    public String getApiKey() {
        return ""; // Not available - stored in database
    }

    public String getClientId() {
        return ""; // Not available - stored in database
    }

    public String getPassword() {
        return ""; // Not available - stored in database
    }

    public String getTotpSecret() {
        return ""; // Not available - stored in database
    }
}
