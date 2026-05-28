package com.financialagent.controller;

import com.financialagent.config.AngelOneConfig;
import com.financialagent.dto.ApiResponse;
import com.financialagent.dto.UICredentialsRequest;
import com.financialagent.service.AngelOneSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for handling UI-based credential management.
 */
@RestController
@RequestMapping("/api/credentials")
@RequiredArgsConstructor
@Validated
public class UICredentialsController {

    private final AngelOneConfig angelOneConfig;
    private final AngelOneSessionService sessionService;

    /**
     * Set credentials from UI.
     */
    @PostMapping("/set")
    public ResponseEntity<ApiResponse<String>> setCredentials(@Valid @RequestBody UICredentialsRequest request) {
        if (!angelOneConfig.isEnableUiLogin()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("UI login is not enabled", null));
        }

        // Clear existing session before setting new credentials
        sessionService.clearSession();

        // Set new credentials
        angelOneConfig.setUiCredentials(
                request.getClientId(),
                request.getPassword(),
                request.getTotpSecret()
        );

        return ResponseEntity.ok(ApiResponse.ok("Credentials set successfully",
                "You can now connect using the Connect button"));
    }

    /**
     * Clear UI credentials.
     */
    @PostMapping("/clear")
    public ResponseEntity<ApiResponse<String>> clearCredentials() {
        if (!angelOneConfig.isEnableUiLogin()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("UI login is not enabled", null));
        }

        sessionService.clearSession();
        angelOneConfig.clearUiCredentials();

        return ResponseEntity.ok(ApiResponse.ok("Credentials cleared", null));
    }

    /**
     * Check if UI login is enabled.
     */
    @PostMapping("/status")
    public ResponseEntity<ApiResponse<UICredentialsStatus>> getCredentialsStatus() {
        UICredentialsStatus status = new UICredentialsStatus();
        status.setUiLoginEnabled(angelOneConfig.isEnableUiLogin());
        status.setHasCredentials(angelOneConfig.isValid());
        status.setIsAuthenticated(sessionService.isAuthenticated());

        return ResponseEntity.ok(ApiResponse.ok(status, "Credentials status"));
    }

    // DTO for response
    public static class UICredentialsStatus {
        private boolean uiLoginEnabled;
        private boolean hasCredentials;
        private boolean isAuthenticated;

        // Getters and setters
        public boolean isUiLoginEnabled() {
            return uiLoginEnabled;
        }

        public void setUiLoginEnabled(boolean uiLoginEnabled) {
            this.uiLoginEnabled = uiLoginEnabled;
        }

        public boolean isHasCredentials() {
            return hasCredentials;
        }

        public void setHasCredentials(boolean hasCredentials) {
            this.hasCredentials = hasCredentials;
        }

        public boolean isAuthenticated() {
            return isAuthenticated;
        }

        public void setIsAuthenticated(boolean authenticated) {
            this.isAuthenticated = authenticated;
        }
    }
}
