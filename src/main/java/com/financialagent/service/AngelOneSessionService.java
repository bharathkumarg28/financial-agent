package com.financialagent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialagent.config.AngelOneConfig;
import com.financialagent.dto.SessionResponse;
import com.financialagent.exception.AuthenticationException;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service for managing AngelOne SmartAPI sessions.
 * Handles authentication, token management, and session lifecycle.
 */
@Service
@RequiredArgsConstructor
public class AngelOneSessionService {

    private static final Logger log = LoggerFactory.getLogger(AngelOneSessionService.class);

    private static final String LOGIN_ENDPOINT = "/rest/auth/angelbroking/user/v1/loginByPassword";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    private static final String ACCEPT_HEADER = "application/json";

    private final AngelOneConfig angelOneConfig;
    private final UserService userService;
    private final WebClient.Builder webClientBuilder;

    // User-specific sessions
    private final ConcurrentHashMap<String, UserSession> userSessions = new ConcurrentHashMap<>();

    // Legacy refs for backward compatibility
    private final AtomicReference<String> jwtTokenRef = new AtomicReference<>();
    private final AtomicReference<String> feedTokenRef = new AtomicReference<>();

    /**
     * Authenticate user with AngelOne SmartAPI using database credentials.
     */
    public String authenticateUser() {
        String username = getCurrentUsername();
        if (username == null) {
            throw new AuthenticationException("No authenticated user found");
        }

        log.info("Authenticating AngelOne for user: {}", username);

        try {
            // Get user credentials from database
            UserService.AngelOneCredentials credentials = userService.getAngelOneCredentials(username);

            // Create login request
            WebClient client = createWebClient();

            // Generate TOTP code from secret
            String totpCode;
            if (credentials.totpSecret() != null && !credentials.totpSecret().trim().isEmpty()) {
                totpCode = generateTotpCode(credentials.totpSecret());
                log.info("Generated TOTP code from secret for user: {}", username);
            } else {
                throw new AuthenticationException("TOTP secret is not configured for user: " + username);
            }

            LoginRequestBody loginRequest = new LoginRequestBody(
                    credentials.clientId(),
                    credentials.password(),
                    totpCode
            );

            // First try to get the response as String to see what's actually returned
            // Add retry logic for SSL handshake timeout
            String rawResponse = client.post()
                    .uri(angelOneConfig.getBaseUrl() + LOGIN_ENDPOINT)
                    .header(HttpHeaders.USER_AGENT, USER_AGENT)
                    .header(HttpHeaders.ACCEPT, ACCEPT_HEADER)
                    .header("X-UserType", "USER")
                    .header("X-SourceID", "WEB")
                    .header("X-ClientLocalIP", "127.0.0.1")
                    .header("X-ClientPublicIP", "127.0.0.1")
                    .header("X-MACAddress", "00:00:00:00:00:00")
                    .header("X-PrivateKey", credentials.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(loginRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(java.time.Duration.ofSeconds(30))
                    .retry(2)
                    .block();

            log.info("AngelOne API Raw Response: {}", rawResponse);

            // Try to parse the response as JSON
            SessionResponse response;
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                response = objectMapper.readValue(rawResponse, SessionResponse.class);
            } catch (Exception e) {
                log.error("Failed to parse AngelOne response as JSON: {}", rawResponse);
                throw new AuthenticationException("AngelOne API returned non-JSON response: " + rawResponse);
            }

            if (!response.isStatus()) {
                throw new AuthenticationException("AngelOne authentication failed: " + response.getMessage());
            }

            // Store user session
            SessionResponse.SessionData sessionData = response.getData();
            UserSession userSession = new UserSession(sessionData.getJwtToken(), sessionData.getFeedToken());
            userSessions.put(username, userSession);

            // Also update legacy refs for backward compatibility
            jwtTokenRef.set(sessionData.getJwtToken());
            feedTokenRef.set(sessionData.getFeedToken());

            log.info("AngelOne authentication successful for user: {}", username);
            return sessionData.getJwtToken();

        } catch (WebClientResponseException e) {
            // Log the actual response content for debugging
            String responseBody = e.getResponseBodyAsString();
            log.error("AngelOne API error for user {}: Status={}, Response body: {}",
                    username, e.getStatusCode(), responseBody);
            throw new AuthenticationException("AngelOne API error: " + responseBody, e);
        } catch (Exception e) {
            log.error("AngelOne authentication failed for user {}: {}", username, e.getMessage());
            throw new AuthenticationException("AngelOne authentication failed: " + e.getMessage(), e);
        }
    }

    /**
     * Authenticate using direct credentials without database storage (session-based).
     */
    public String authenticateWithCredentials(String clientId, String password, String apiKey, String totp) {
        log.info("Authenticating AngelOne with direct credentials (session-based)");

        try {
            WebClient client = createWebClient();

            LoginRequestBody loginRequest = new LoginRequestBody(
                    clientId,
                    password,
                    totp
            );

            String rawResponse = client.post()
                    .uri(angelOneConfig.getBaseUrl() + LOGIN_ENDPOINT)
                    .header(HttpHeaders.USER_AGENT, USER_AGENT)
                    .header(HttpHeaders.ACCEPT, ACCEPT_HEADER)
                    .header("X-UserType", "USER")
                    .header("X-SourceID", "WEB")
                    .header("X-ClientLocalIP", "127.0.0.1")
                    .header("X-ClientPublicIP", "127.0.0.1")
                    .header("X-MACAddress", "00:00:00:00:00:00")
                    .header("X-PrivateKey", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(loginRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(java.time.Duration.ofSeconds(30))
                    .retry(2)
                    .block();

            log.info("AngelOne API Raw Response: {}", rawResponse);

            SessionResponse response;
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                response = objectMapper.readValue(rawResponse, SessionResponse.class);
            } catch (Exception e) {
                log.error("Failed to parse AngelOne response as JSON: {}", rawResponse);
                throw new AuthenticationException("AngelOne API returned non-JSON response: " + rawResponse);
            }

            if (!response.isStatus()) {
                throw new AuthenticationException("AngelOne authentication failed: " + response.getMessage());
            }

            SessionResponse.SessionData sessionData = response.getData();
            
            // Store in legacy refs for session-based access
            jwtTokenRef.set(sessionData.getJwtToken());
            feedTokenRef.set(sessionData.getFeedToken());

            log.info("AngelOne authentication successful with direct credentials");
            return sessionData.getJwtToken();

        } catch (WebClientResponseException e) {
            String responseBody = e.getResponseBodyAsString();
            log.error("AngelOne API error: Status={}, Response body: {}", e.getStatusCode(), responseBody);
            throw new AuthenticationException("AngelOne API error: " + responseBody, e);
        } catch (Exception e) {
            log.error("AngelOne authentication failed: {}", e.getMessage());
            throw new AuthenticationException("AngelOne authentication failed: " + e.getMessage(), e);
        }
    }

    /**
     * Legacy authenticate method for backward compatibility.
     */
    public String authenticate() {
        // Use user-specific authentication when username is available
        String username = getCurrentUsername();
        if (username != null) {
            return authenticateUser();
        }

        // Fallback to legacy authentication
        return authenticateLegacy();
    }

    /**
     * Get JWT token for current user.
     */
    public String getJwtToken() {
        String username = getCurrentUsername();
        if (username != null) {
            UserSession session = userSessions.get(username);
            if (session != null && !session.isExpired()) {
                session.updateLastUsed();
                return session.jwtToken;
            }
        }

        // Fallback to legacy token
        return jwtTokenRef.get();
    }

    /**
     * Get feed token for current user.
     */
    public String getFeedToken() {
        String username = getCurrentUsername();
        if (username != null) {
            UserSession session = userSessions.get(username);
            if (session != null && !session.isExpired()) {
                session.updateLastUsed();
                return session.feedToken;
            }
        }

        // Fallback to legacy token
        return feedTokenRef.get();
    }

    /**
     * Check if user is authenticated with AngelOne.
     */
    public boolean isAuthenticated() {
        String username = getCurrentUsername();
        if (username != null) {
            UserSession session = userSessions.get(username);
            return session != null && !session.isExpired();
        }

        // Fallback to legacy check
        return jwtTokenRef.get() != null;
    }

    /**
     * Clear session for current user.
     */
    public void clearSession() {
        String username = getCurrentUsername();
        if (username != null) {
            userSessions.remove(username);
        }

        // Clear legacy refs
        jwtTokenRef.set(null);
        feedTokenRef.set(null);

        log.info("Session cleared");
    }

    /**
     * Creates a WebClient configured with authentication headers.
     */
    public WebClient createAuthenticatedClient() {
        String token = getJwtToken();
        String username = getCurrentUsername();
        String apiKey = "";

        // Get user-specific API key from database
        if (username != null) {
            try {
                UserService.AngelOneCredentials credentials = userService.getAngelOneCredentials(username);
                apiKey = credentials.apiKey();
            } catch (Exception e) {
                log.warn("Failed to get API key for user {}, checking session: {}", username, e.getMessage());
                // Check if API key is stored in session (for TOTP-based login)
                try {
                    jakarta.servlet.http.HttpSession session = 
                        ((org.springframework.web.context.request.ServletRequestAttributes) 
                            org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes())
                            .getRequest().getSession(false);
                    if (session != null) {
                        String sessionApiKey = (String) session.getAttribute("angelOneApiKey");
                        if (sessionApiKey != null) {
                            apiKey = sessionApiKey;
                            log.info("Using API key from session for user: {}", username);
                        }
                    }
                } catch (Exception sessionEx) {
                    log.warn("Failed to get API key from session: {}", sessionEx.getMessage());
                }
                if (apiKey.isEmpty()) {
                    apiKey = angelOneConfig.getApiKey(); // Fallback to legacy (will be empty)
                }
            }
        }

        return webClientBuilder
                .baseUrl(angelOneConfig.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, ACCEPT_HEADER)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .defaultHeader("X-UserType", "USER")
                .defaultHeader("X-SourceID", "WEB")
                .defaultHeader("X-ClientLocalIP", "127.0.0.1")
                .defaultHeader("X-ClientPublicIP", "127.0.0.1")
                .defaultHeader("X-MACAddress", "00:00:00:00:00:00")
                .defaultHeader("X-PrivateKey", apiKey)
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .build();
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            return auth.getName();
        }
        return null;
    }

    private String generateTotpCode(String totpSecret) {
        try {
            TimeProvider timeProvider = new SystemTimeProvider();
            CodeGenerator codeGenerator = new DefaultCodeGenerator();
            long currentBucket = Math.floorDiv(timeProvider.getTime(), 30);
            return codeGenerator.generate(totpSecret, currentBucket);

        } catch (Exception e) {
            log.error("Failed to generate TOTP code: {}", e.getMessage());
            throw new AuthenticationException("Failed to generate TOTP code", e);
        }
    }


    private WebClient createWebClient() {
        return webClientBuilder
                .baseUrl(angelOneConfig.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .build();
    }

    private String authenticateLegacy() {
        log.info("Using legacy AngelOne authentication with environment variables");

        if (!angelOneConfig.isValid()) {
            throw new AuthenticationException("AngelOne credentials not configured in environment variables");
        }

        try {
            WebClient client = createWebClient();

            String totpCode = generateTotpCodeLegacy();

            SessionResponse.LoginRequest loginRequest = new SessionResponse.LoginRequest(
                    angelOneConfig.getClientId(),
                    angelOneConfig.getPassword(),
                    totpCode
            );

            SessionResponse response = client.post()
                    .uri(angelOneConfig.getBaseUrl() + LOGIN_ENDPOINT)
                    .header(HttpHeaders.USER_AGENT, USER_AGENT)
                    .header(HttpHeaders.ACCEPT, ACCEPT_HEADER)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(loginRequest)
                    .retrieve()
                    .bodyToMono(SessionResponse.class)
                    .block();

            if (!response.isStatus()) {
                throw new AuthenticationException("AngelOne authentication failed: " + response.getMessage());
            }

            SessionResponse.SessionData sessionData = response.getData();
            jwtTokenRef.set(sessionData.getJwtToken());
            feedTokenRef.set(sessionData.getFeedToken());

            log.info("AngelOne legacy authentication successful");
            return sessionData.getJwtToken();

        } catch (WebClientResponseException e) {
            log.error("AngelOne API error: {}", e.getResponseBodyAsString());
            throw new AuthenticationException("AngelOne API error: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("AngelOne legacy authentication failed: {}", e.getMessage());
            throw new AuthenticationException("AngelOne legacy authentication failed", e);
        }
    }

    private String generateTotpCodeLegacy() {
        try {
            String totpSecret = angelOneConfig.getTotpSecret();
            if (totpSecret == null || totpSecret.isEmpty()) {
                throw new AuthenticationException("TOTP secret not configured");
            }

            return generateTotpCode(totpSecret);

        } catch (Exception e) {
            log.error("Failed to generate legacy TOTP code: {}", e.getMessage());
            throw new AuthenticationException("Failed to generate legacy TOTP code", e);
        }
    }

    /**
     * User session data holder.
     */
    private static class UserSession {
        private final String jwtToken;
        private final String feedToken;
        private final LocalDateTime createdAt;
        private volatile LocalDateTime lastUsed;

        public UserSession(String jwtToken, String feedToken) {
            this.jwtToken = jwtToken;
            this.feedToken = feedToken;
            this.createdAt = LocalDateTime.now();
            this.lastUsed = LocalDateTime.now();
        }

        public boolean isExpired() {
            // Sessions expire after 24 hours
            return lastUsed.isBefore(LocalDateTime.now().minusHours(24));
        }

        public void updateLastUsed() {
            this.lastUsed = LocalDateTime.now();
        }
    }

    // Inner class for login request body
    private record LoginRequestBody(String clientcode, String password, String totp) {
    }
}