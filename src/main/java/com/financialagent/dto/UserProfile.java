package com.financialagent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for user profile information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String role;
    private Boolean enabled;
    private LocalDateTime lastLogin;
    private Integer loginCount;
    private String theme;
    private Boolean notificationsEnabled;
    private LocalDateTime createdAt;

    // Static builder method as workaround for Lombok annotation processing issue
    public static UserProfileBuilder builder() {
        return new UserProfileBuilder();
    }

    public static class UserProfileBuilder {
        private Long id;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private String phoneNumber;
        private String role;
        private Boolean enabled;
        private LocalDateTime lastLogin;
        private Integer loginCount;
        private String theme;
        private Boolean notificationsEnabled;
        private LocalDateTime createdAt;

        public UserProfileBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UserProfileBuilder username(String username) {
            this.username = username;
            return this;
        }

        public UserProfileBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserProfileBuilder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public UserProfileBuilder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public UserProfileBuilder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public UserProfileBuilder role(String role) {
            this.role = role;
            return this;
        }

        public UserProfileBuilder enabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public UserProfileBuilder lastLogin(LocalDateTime lastLogin) {
            this.lastLogin = lastLogin;
            return this;
        }

        public UserProfileBuilder loginCount(Integer loginCount) {
            this.loginCount = loginCount;
            return this;
        }

        public UserProfileBuilder theme(String theme) {
            this.theme = theme;
            return this;
        }

        public UserProfileBuilder notificationsEnabled(Boolean notificationsEnabled) {
            this.notificationsEnabled = notificationsEnabled;
            return this;
        }

        public UserProfileBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserProfile build() {
            UserProfile profile = new UserProfile();
            profile.id = this.id;
            profile.username = this.username;
            profile.email = this.email;
            profile.firstName = this.firstName;
            profile.lastName = this.lastName;
            profile.phoneNumber = this.phoneNumber;
            profile.role = this.role;
            profile.enabled = this.enabled;
            profile.lastLogin = this.lastLogin;
            profile.loginCount = this.loginCount;
            profile.theme = this.theme;
            profile.notificationsEnabled = this.notificationsEnabled;
            profile.createdAt = this.createdAt;
            return profile;
        }
    }
}
