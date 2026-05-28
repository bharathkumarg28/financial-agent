package com.financialagent.controller;

import com.financialagent.config.AngelOneConfig;
import com.financialagent.dto.ApiResponse;
import com.financialagent.entity.User;
import com.financialagent.repository.UserRepository;
import com.financialagent.service.AngelOneSessionService;
import com.financialagent.service.EncryptionService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.UUID;

/**
 * Controller for TOTP-based login without database storage.
 * When enabled via YAML configuration, allows direct login with clientId, password, apiKey, and TOTP.
 */
@Controller
@RequiredArgsConstructor
public class TotpLoginController {

    private final AngelOneConfig angelOneConfig;
    private final AngelOneSessionService angelOneSessionService;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Show TOTP-based login page.
     */
    @GetMapping("/totp-login")
    public String totpLoginPage(Model model) {
        if (!angelOneConfig.isTotpLoginEnabled()) {
            return "redirect:/login";
        }
        model.addAttribute("enabled", true);
        return "totp-login";
    }

    /**
     * Authenticate user with TOTP-based login.
     * Creates a user in DB if not found and stores authentication in session.
     */
    @PostMapping("/api/totp-login/authenticate")
    @ResponseBody
    public ApiResponse<String> authenticate(
            @RequestParam String clientId,
            @RequestParam String password,
            @RequestParam String apiKey,
            @RequestParam String totp,
            HttpSession session) {
        try {
            // Authenticate with AngelOne using direct credentials (TOTP code directly, not secret)
            String jwtToken = angelOneSessionService.authenticateWithCredentials(
                    clientId, password, apiKey, totp);

            // Check if user exists in DB by clientId (AngelOne client ID)
            User user = userRepository.findByAngelOneClientId(clientId).orElse(null);
            
            if (user == null) {
                // Create user in DB if not found
                user = User.builder()
                        .username(clientId)
                        .email(clientId + "@totp-user.local") // Placeholder email for TOTP users
                        .password(passwordEncoder.encode(UUID.randomUUID().toString())) // Random password
                        .firstName("TOTP") // Placeholder first name
                        .angelOneClientId(clientId)
                        .angelOneApiKey(encryptionService.encrypt(apiKey))
                        .angelOnePassword(encryptionService.encrypt(password))
                        .angelOneTotpSecret(null) // No TOTP secret stored for TOTP login users
                        .enabled(true)
                        .accountNonExpired(true)
                        .accountNonLocked(true)
                        .credentialsNonExpired(true)
                        .role(User.UserRole.USER)
                        .loginCount(0)
                        .failedLoginAttempts(0)
                        .theme("dark")
                        .notificationsEnabled(true)
                        .build();
                
                user = userRepository.save(user);
            }

            // Create temporary Spring Security authentication for session-based access
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    clientId,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            
            // Store the security context in the session
            session.setAttribute("SPRING_SECURITY_CONTEXT", context);

            // Store AngelOne credentials in session for later use
            session.setAttribute("angelOneClientId", clientId);
            session.setAttribute("angelOneApiKey", apiKey);

            return ApiResponse.ok(jwtToken, "Authentication successful");
        } catch (Exception e) {
            return ApiResponse.fail("Authentication failed", e.getMessage());
        }
    }

    /**
     * Check TOTP-based login status.
     */
    @GetMapping("/api/totp-login/status")
    @ResponseBody
    public ApiResponse<Boolean> getStatus() {
        return ApiResponse.ok(angelOneConfig.isTotpLoginEnabled(), "TOTP login status retrieved");
    }
}
