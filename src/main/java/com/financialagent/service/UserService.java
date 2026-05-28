package com.financialagent.service;

import com.financialagent.dto.UserRegistrationDto;
import com.financialagent.entity.User;
import com.financialagent.exception.DuplicateResourceException;
import com.financialagent.exception.ResourceNotFoundException;
import com.financialagent.exception.UserAccountException;
import com.financialagent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for user management operations including registration, authentication, and profile management.
 */
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EncryptionService encryptionService;

    /**
     * Register a new user with AngelOne credentials.
     */
    @Transactional
    public User registerUser(UserRegistrationDto registrationDto) {
        log.info("Registering new user: {}", registrationDto.getUsername());

        // Check for existing username
        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + registrationDto.getUsername());
        }

        // Check for existing email
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + registrationDto.getEmail());
        }

        // Check for existing AngelOne client ID
        if (userRepository.existsByAngelOneClientId(registrationDto.getAngelOneClientId())) {
            throw new DuplicateResourceException("AngelOne client ID already exists: " + registrationDto.getAngelOneClientId());
        }

        // Create new user
        User user = new User(
                registrationDto.getUsername(),
                registrationDto.getEmail(),
                passwordEncoder.encode(registrationDto.getPassword()),
                registrationDto.getFirstName(),
                registrationDto.getLastName(),
                registrationDto.getPhoneNumber(),
                registrationDto.getAngelOneApiKey(),
                registrationDto.getAngelOneClientId(),
                encryptionService.encrypt(registrationDto.getAngelOnePassword()),
                registrationDto.getAngelOneTotpSecret() != null ?
                        encryptionService.encrypt(registrationDto.getAngelOneTotpSecret()) : null
        );
        user.setEnabled(true);
        user.setRole(User.UserRole.USER);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        user.setNotificationsEnabled(true);
        user.setTheme("dark");

        // Save user
        User savedUser = userRepository.save(user);
        log.info("Successfully registered user: {}", savedUser.getUsername());

        return savedUser;
    }

    /**
     * Authenticate user with username and password.
     */
    @Transactional
    public User authenticateUser(String username, String password) {
        log.debug("Authenticating user: {}", username);

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        User user = userOpt.get();

        // Check if account is locked
        if (user.isLocked()) {
            throw new UserAccountException("Account is locked. Please try again later.");
        }

        // Check if account is enabled
        if (!user.isEnabled()) {
            throw new UserAccountException("Account is disabled. Please contact support.");
        }

        // Verify password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            // Increment failed login attempts
            user.incrementFailedLoginAttempts();
            userRepository.save(user);

            log.warn("Failed login attempt for user: {}, attempts: {}",
                    username, user.getFailedLoginAttempts());

            throw new UserAccountException("Invalid credentials");
        }

        // Successful login
        user.incrementLoginCount();
        userRepository.save(user);

        log.info("User authenticated successfully: {}", username);
        return user;
    }

    /**
     * Get user by ID.
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }

    /**
     * Get user by username.
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    /**
     * Update user profile.
     */
    @Transactional
    public User updateUserProfile(Long userId, User updatedUser) {
        User existingUser = getUserById(userId);

        // Update allowed fields
        existingUser.setFirstName(updatedUser.getFirstName());
        existingUser.setLastName(updatedUser.getLastName());
        existingUser.setPhoneNumber(updatedUser.getPhoneNumber());
        existingUser.setTheme(updatedUser.getTheme());
        existingUser.setNotificationsEnabled(updatedUser.getNotificationsEnabled());

        return userRepository.save(existingUser);
    }

    /**
     * Update AngelOne credentials.
     */
    @Transactional
    public User updateAngelOneCredentials(Long userId, String apiKey, String clientId,
                                          String password, String totpSecret) {
        User user = getUserById(userId);

        // Check if another user has the same client ID
        if (!user.getAngelOneClientId().equals(clientId)) {
            if (userRepository.existsByAngelOneClientId(clientId)) {
                throw new DuplicateResourceException("AngelOne client ID already exists: " + clientId);
            }
        }

        user.setAngelOneApiKey(apiKey);
        user.setAngelOneClientId(clientId);
        user.setAngelOnePassword(encryptionService.encrypt(password));
        user.setAngelOneTotpSecret(encryptionService.encrypt(totpSecret));

        return userRepository.save(user);
    }

    /**
     * Change user password.
     */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getUserById(userId);

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new UserAccountException("Current password is incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password changed for user: {}", user.getUsername());
    }

    /**
     * Delete user account.
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = getUserById(userId);

        // Soft delete by disabling account
        user.setEnabled(false);
        userRepository.save(user);

        log.info("User account disabled: {}", user.getUsername());
    }

    /**
     * Search users.
     */
    public Page<User> searchUsers(String query, Pageable pageable) {
        return userRepository.searchUsers(query, pageable);
    }

    /**
     * Get user statistics.
     */
    public UserStatistics getUserStatistics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minusDays(30);

        Object[] stats = userRepository.getUserStatistics(thirtyDaysAgo, now);

        return new UserStatistics(
                (Long) stats[0],
                (Long) stats[1],
                (Long) stats[2],
                (Long) stats[3]
        );
    }

    /**
     * Reset failed login attempts for a user.
     */
    @Transactional
    public void resetFailedLoginAttempts(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.resetFailedLoginAttempts();
            userRepository.save(user);
            log.info("Reset failed login attempts for user: {}", username);
        }
    }

    /**
     * Load user by username for Spring Security.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        log.debug("Loaded user for authentication: {}", username);
        return user;
    }

    /**
     * Get decrypted AngelOne credentials for a user.
     */
    public AngelOneCredentials getAngelOneCredentials(String username) {
        User user = getUserByUsername(username);

        String totpSecret = user.getAngelOneTotpSecret() != null ?
                encryptionService.decrypt(user.getAngelOneTotpSecret()) : null;

        return new AngelOneCredentials(
                user.getAngelOneApiKey(),
                user.getAngelOneClientId(),
                encryptionService.decrypt(user.getAngelOnePassword()),
                totpSecret
        );
    }

    /**
     * User statistics record.
     */
    public record UserStatistics(
            Long totalUsers,
            Long newUsers,
            Long activeUsers,
            Long lockedUsers
    ) {
    }

    /**
     * AngelOne credentials record.
     */
    /**
     * Check if user has complete AngelOne credentials.
     */
    public boolean hasAngelOneCredentials(String username) {
        try {
            User user = getUserByUsername(username);
            boolean hasBasicCreds = user.getAngelOneApiKey() != null && !user.getAngelOneApiKey().trim().isEmpty() &&
                    user.getAngelOneClientId() != null && !user.getAngelOneClientId().trim().isEmpty() &&
                    user.getAngelOnePassword() != null && !user.getAngelOnePassword().trim().isEmpty();
            boolean hasTotpOption = user.getAngelOneTotpSecret() != null && !user.getAngelOneTotpSecret().trim().isEmpty();
            return hasBasicCreds && hasTotpOption;
        } catch (Exception e) {
            log.error("Error checking AngelOne credentials for user {}: {}", username, e.getMessage());
            return false;
        }
    }

    public record AngelOneCredentials(
            String apiKey,
            String clientId,
            String password,
            String totpSecret
    ) {
    }
}
