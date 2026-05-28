package com.financialagent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO for receiving credentials from UI.
 */
@Data
public class UICredentialsRequest {

    @NotBlank(message = "Client ID is required")
    private String clientId;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "TOTP Secret is required")
    private String totpSecret;

    // Getter methods as workaround for Lombok annotation processing issue
    public String getClientId() {
        return clientId;
    }

    public String getPassword() {
        return password;
    }

    public String getTotpSecret() {
        return totpSecret;
    }
}
