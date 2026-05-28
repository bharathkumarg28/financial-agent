package com.financialagent.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Service for handling authentication-related operations including AngelOne session management.
 */
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final AngelOneSessionService angelOneSessionService;
    private final UserService userService;

    /**
     * Establish AngelOne session for a user after successful login.
     * This is called synchronously during login to ensure session is established.
     */
    public void establishAngelOneSession(String username) {
        try {
            log.info("Starting AngelOne session establishment for user: {}", username);
            log.info("Thread: {}", Thread.currentThread().getName());

            // Check if user has AngelOne credentials
            boolean hasCredentials = userService.hasAngelOneCredentials(username);
            log.info("User {} has AngelOne credentials: {}", username, hasCredentials);

            if (!hasCredentials) {
                log.info("User {} does not have AngelOne credentials configured", username);
                return;
            }

            // Check if session is already established
            if (angelOneSessionService.isAuthenticated()) {
                log.info("AngelOne session already established for user: {}", username);
                return;
            }

            // Establish AngelOne session
            log.info("Calling AngelOne authentication for user: {}", username);
            String jwtToken = angelOneSessionService.authenticate();
            log.info("JWT token result: {}", jwtToken != null ? "SUCCESS" : "NULL");

            String feedToken = angelOneSessionService.getFeedToken();
            log.info("Feed token result: {}", feedToken != null ? "SUCCESS" : "NULL");

            if (jwtToken != null && feedToken != null) {
                log.info("Successfully established AngelOne session for user: {}", username);
            } else {
                log.warn("Failed to establish AngelOne session for user: {} - JWT: {}, Feed: {}",
                        username, jwtToken != null ? "OK" : "NULL", feedToken != null ? "OK" : "NULL");
            }

        } catch (Exception e) {
            log.error("Error establishing AngelOne session for user {}: {}", username, e.getMessage(), e);
        }
    }

    /**
     * Ensure AngelOne session is established for the current user.
     * This method can be called when needed to establish session for already logged-in users.
     */
    public boolean ensureAngelOneSession() {
        try {
            // Get current authenticated user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
                log.warn("No authenticated user found for AngelOne session establishment");
                return false;
            }

            String username = auth.getName();

            // Check if session is already established
            if (angelOneSessionService.isAuthenticated()) {
                log.info("AngelOne session already established for user: {}", username);
                return true;
            }

            // Establish session
            establishAngelOneSession(username);
            return angelOneSessionService.isAuthenticated();

        } catch (Exception e) {
            log.error("Error ensuring AngelOne session: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Invalidate AngelOne session when user logs out.
     * Uses the same method as the disconnect button.
     */
    public void invalidateAngelOneSession(String username) {
        try {
            log.info("Invalidating AngelOne session for user: {}", username);

            // Clear the session using the same method as the disconnect button
            angelOneSessionService.clearSession();

            log.info("Successfully invalidated AngelOne session for user: {}", username);
        } catch (Exception e) {
            log.error("Error invalidating AngelOne session for user {}: {}", username, e.getMessage());
        }
    }
}
