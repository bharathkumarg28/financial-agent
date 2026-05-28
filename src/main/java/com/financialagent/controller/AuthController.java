package com.financialagent.controller;

import com.financialagent.dto.ApiResponse;
import com.financialagent.dto.UserRegistrationDto;
import com.financialagent.exception.DuplicateResourceException;
import com.financialagent.exception.UserAccountException;
import com.financialagent.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for authentication operations including registration, login, and logout.
 */
@Controller
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;

    /**
     * Display registration page.
     */
    @GetMapping("/register")
    public String showRegistrationPage(Model model) {
        model.addAttribute("userRegistrationDto", new UserRegistrationDto());
        return "register";
    }

    /**
     * Process user registration.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> registerUser(
            @Valid @RequestBody UserRegistrationDto registrationDto,
            BindingResult bindingResult,
            HttpServletRequest request) {

        log.info("Processing registration for user: {}", registrationDto.getUsername());

        try {
            // Validate form data
            if (bindingResult.hasErrors()) {
                String errorMessage = bindingResult.getFieldErrors().stream()
                        .map(error -> error.getField() + ": " + error.getDefaultMessage())
                        .findFirst()
                        .orElse("Invalid form data");
                return ResponseEntity.badRequest()
                        .body(ApiResponse.fail(errorMessage));
            }

            // Check password confirmation
            if (!registrationDto.isPasswordMatching()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.fail("Password and confirm password do not match"));
            }

            // Check terms agreement
            if (!registrationDto.isAgreeToTerms()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.fail("You must agree to the terms and conditions"));
            }

            // Register user
            userService.registerUser(registrationDto);

            log.info("User registered successfully: {}", registrationDto.getUsername());
            return ResponseEntity.ok(ApiResponse.ok("Registration successful! Please login to continue."));

        } catch (DuplicateResourceException e) {
            log.warn("Registration failed - duplicate resource: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            log.error("Registration failed for user: {}", registrationDto.getUsername(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail("Registration failed. Please try again."));
        }
    }

    /**
     * Display login page.
     */
    @GetMapping("/login")
    public String showLoginPage(@RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout,
                                Model model) {

        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }

        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
        }

        return "login";
    }

    /**
     * Process user login (handled by Spring Security).
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> loginUser(
            @RequestParam String username,
            @RequestParam String password,
            HttpServletRequest request) {

        log.info("Processing login for user: {}", username);

        try {
            // Authenticate user
            userService.authenticateUser(username, password);

            // Create session
            HttpSession session = request.getSession(true);
            session.setAttribute("username", username);

            log.info("User logged in successfully: {}", username);
            return ResponseEntity.ok(ApiResponse.ok("Login successful"));

        } catch (UserAccountException e) {
            log.warn("Login failed for user {}: {}", username, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            log.error("Login failed for user: {}", username, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail("Login failed. Please try again."));
        }
    }

    /**
     * Process user logout.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logoutUser(HttpServletRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            String username = auth.getName();
            log.info("Logging out user: {}", username);

            // Invalidate session
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }

            // Clear security context
            SecurityContextHolder.clearContext();
        }

        return ResponseEntity.ok(ApiResponse.ok("Logout successful"));
    }

    /**
     * Check if user is authenticated.
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<AuthStatus>> getAuthStatus(Authentication auth) {

        if (auth != null && auth.isAuthenticated()) {
            AuthStatus status = new AuthStatus(
                    true,
                    auth.getName(),
                    auth.getAuthorities().stream()
                            .map(Object::toString)
                            .toList()
            );
            return ResponseEntity.ok(ApiResponse.ok(status, "User is authenticated"));
        } else {
            AuthStatus status = new AuthStatus(
                    false,
                    null,
                    null
            );
            return ResponseEntity.ok(ApiResponse.ok(status, "User is not authenticated"));
        }
    }

    /**
     * Get current user profile.
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<com.financialagent.dto.UserProfile>> getUserProfile(Authentication auth) {

        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("User is not authenticated"));
        }

        try {
            var user = userService.getUserByUsername(auth.getName());

            com.financialagent.dto.UserProfile profile = com.financialagent.dto.UserProfile.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .phoneNumber(user.getPhoneNumber())
                    .role(user.getRole().name())
                    .enabled(user.isEnabled())
                    .lastLogin(user.getLastLogin())
                    .loginCount(user.getLoginCount())
                    .theme(user.getTheme())
                    .notificationsEnabled(user.getNotificationsEnabled())
                    .createdAt(user.getCreatedAt())
                    .build();

            return ResponseEntity.ok(ApiResponse.ok(profile, "User profile retrieved successfully"));

        } catch (Exception e) {
            log.error("Failed to get user profile for: {}", auth.getName(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail("Failed to retrieve user profile"));
        }
    }

    /**
     * Change password.
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            Authentication auth) {

        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("User is not authenticated"));
        }

        try {
            var user = userService.getUserByUsername(auth.getName());
            userService.changePassword(user.getId(), currentPassword, newPassword);

            return ResponseEntity.ok(ApiResponse.ok("Password changed successfully"));

        } catch (UserAccountException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to change password for user: {}", auth.getName(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail("Failed to change password"));
        }
    }

    /**
     * Reset failed login attempts.
     */
    @PostMapping("/reset-failed-attempts")
    public ResponseEntity<ApiResponse<String>> resetFailedAttempts(@RequestParam String username) {

        try {
            userService.resetFailedLoginAttempts(username);
            return ResponseEntity.ok(ApiResponse.ok("Failed login attempts reset successfully"));

        } catch (Exception e) {
            log.error("Failed to reset attempts for user: {}", username, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail("Failed to reset failed login attempts"));
        }
    }

    /**
     * Auth status record.
     */
    public record AuthStatus(
            boolean authenticated,
            String username,
            java.util.List<String> authorities
    ) {
    }

    /**
     * User profile record.
     */
    public record UserProfile(
            Long id,
            String username,
            String email,
            String firstName,
            String lastName,
            String phoneNumber,
            String role,
            boolean enabled,
            java.time.LocalDateTime lastLogin,
            Integer loginCount,
            String theme,
            boolean notificationsEnabled,
            java.time.LocalDateTime createdAt
    ) {
    }
}
