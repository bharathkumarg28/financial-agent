package com.financialagent.controller;

import com.financialagent.config.AngelOneConfig;
import com.financialagent.dto.ApiResponse;
import com.financialagent.service.AngelOneSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for AngelOne SmartAPI connection management.
 * Equivalent to connect.py functionality.
 */
@RestController
@RequestMapping("/api/connection")
@RequiredArgsConstructor
public class ConnectionController {

    private final AngelOneSessionService sessionService;
    private final AngelOneConfig angelOneConfig;

    /**
     * Test connection and authenticate with AngelOne SmartAPI.
     */
    @PostMapping("/connect")
    public ResponseEntity<ApiResponse<ConnectionStatus>> connect() {
        // Check if UI login is enabled but credentials are not set
        if (angelOneConfig.isEnableUiLogin() && !angelOneConfig.isValidForUiLogin()) {
            ConnectionStatus status = new ConnectionStatus(
                    false,
                    null,
                    null,
                    "Redirect to settings page to set credentials"
            );
            return ResponseEntity.ok(ApiResponse.ok(status, "redirect_to_settings"));
        }

        try {
            String token = sessionService.authenticate();
            String feedToken = sessionService.getFeedToken();

            ConnectionStatus status = new ConnectionStatus(
                    true,
                    token.substring(0, Math.min(20, token.length())) + "...",
                    feedToken,
                    "Connected successfully"
            );

            return ResponseEntity.ok(ApiResponse.ok(status, "Login successful!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Authentication failed", e.getMessage()));
        }
    }

    /**
     * Check current connection status.
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<ConnectionStatus>> getStatus() {
        boolean isConnected = sessionService.isAuthenticated();

        ConnectionStatus status = new ConnectionStatus(
                isConnected,
                isConnected ? "Active" : null,
                isConnected ? sessionService.getFeedToken() : null,
                isConnected ? "Connected" : "Not connected"
        );

        return ResponseEntity.ok(ApiResponse.ok(status));
    }

    /**
     * Disconnect and clear session.
     */
    @PostMapping("/disconnect")
    public ResponseEntity<ApiResponse<String>> disconnect() {
        sessionService.clearSession();
        return ResponseEntity.ok(ApiResponse.ok("Disconnected", "Session cleared successfully"));
    }

    // Response record for connection status
    public record ConnectionStatus(
            boolean connected,
            String sessionToken,
            String feedToken,
            String message
    ) {
    }
}
