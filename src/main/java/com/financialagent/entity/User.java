package com.financialagent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * User entity for authentication and storing AngelOne credentials.
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_client_id", columnList = "angelOneClientId"),
        @Index(name = "idx_user_username", columnList = "username")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    @Column(nullable = false)
    private String password;

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Size(max = 50, message = "Last name must not exceed 50 characters")
    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "telegram_chat_id", length = 100)
    private String telegramChatId;

    // AngelOne Credentials (encrypted)
    @NotBlank(message = "AngelOne API key is required")
    @Column(name = "angel_one_api_key", nullable = false)
    private String angelOneApiKey;

    @NotBlank(message = "AngelOne client ID is required")
    @Column(name = "angel_one_client_id", nullable = false, length = 50)
    private String angelOneClientId;

    @NotBlank(message = "AngelOne password is required")
    @Column(name = "angel_one_password", nullable = false)
    private String angelOnePassword;

    @Column(name = "angel_one_totp_secret")
    private String angelOneTotpSecret;

    // Account Status
    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean accountNonExpired = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean accountNonLocked = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean credentialsNonExpired = true;

    // User Role
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserRole role = UserRole.USER;

    // Session Information
    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "login_count")
    @Builder.Default
    private Integer loginCount = 0;

    @Column(name = "failed_login_attempts")
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    // Preferences
    @Column(name = "theme")
    @Builder.Default
    private String theme = "dark";

    @Column(name = "notifications_enabled")
    @Builder.Default
    private Boolean notificationsEnabled = true;

    // Audit fields
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Override UserDetails methods
    @Override
    public Set<GrantedAuthority> getAuthorities() {
        return Set.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked && (lockedUntil == null || lockedUntil.isBefore(LocalDateTime.now()));
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    // Constructor to replace builder pattern
    public User(String username, String email, String password, String firstName, String lastName,
                String phoneNumber, String angelOneApiKey, String angelOneClientId,
                String angelOnePassword, String angelOneTotpSecret) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.angelOneApiKey = angelOneApiKey;
        this.angelOneClientId = angelOneClientId;
        this.angelOnePassword = angelOnePassword;
        this.angelOneTotpSecret = angelOneTotpSecret;
    }

    // Explicit getters and setters since Lombok @Data isn't working
    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getTelegramChatId() {
        return telegramChatId;
    }

    public void setTelegramChatId(String telegramChatId) {
        this.telegramChatId = telegramChatId;
    }

    public UserRole getRole() {
        return role;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public Integer getLoginCount() {
        return loginCount;
    }

    public String getTheme() {
        return theme;
    }

    public Boolean getNotificationsEnabled() {
        return notificationsEnabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getAngelOneApiKey() {
        return angelOneApiKey;
    }

    public String getAngelOneClientId() {
        return angelOneClientId;
    }

    public String getAngelOnePassword() {
        return angelOnePassword;
    }

    public String getAngelOneTotpSecret() {
        return angelOneTotpSecret;
    }

    public String getPassword() {
        return password;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public void setNotificationsEnabled(Boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setAngelOneApiKey(String angelOneApiKey) {
        this.angelOneApiKey = angelOneApiKey;
    }

    public void setAngelOneClientId(String angelOneClientId) {
        this.angelOneClientId = angelOneClientId;
    }

    public void setAngelOnePassword(String angelOnePassword) {
        this.angelOnePassword = angelOnePassword;
    }

    public void setAngelOneTotpSecret(String angelOneTotpSecret) {
        this.angelOneTotpSecret = angelOneTotpSecret;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setFailedLoginAttempts(Integer failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public Integer getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    // Helper methods
    public String getFullName() {
        if (lastName != null && !lastName.trim().isEmpty()) {
            return firstName + " " + lastName;
        }
        return firstName;
    }

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    public void incrementLoginCount() {
        this.loginCount = (this.loginCount == null ? 0 : this.loginCount) + 1;
        this.lastLogin = LocalDateTime.now();
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts = (this.failedLoginAttempts == null ? 0 : this.failedLoginAttempts) + 1;

        // Lock account after 5 failed attempts for 30 minutes
        if (this.failedLoginAttempts >= 5) {
            this.lockedUntil = LocalDateTime.now().plusMinutes(30);
        }
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    /**
     * User roles enumeration.
     */
    public enum UserRole {
        USER,
        ADMIN,
        MODERATOR
    }
}
