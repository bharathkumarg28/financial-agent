package com.financialagent.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for user registration.
 */
public class UserRegistrationDto {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message = "Password must contain at least one uppercase letter, one lowercase letter, and one digit")
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Phone number must be valid")
    private String phoneNumber;

    // AngelOne Credentials
    @NotBlank(message = "AngelOne API key is required")
    private String angelOneApiKey;

    @NotBlank(message = "AngelOne client ID is required")
    @Size(min = 5, max = 50, message = "AngelOne client ID must be between 5 and 50 characters")
    private String angelOneClientId;

    @NotBlank(message = "AngelOne password is required")
    @Size(min = 4, max = 6, message = "AngelOne password must be 4-6 digits")
    @Pattern(regexp = "^[0-9]{4,6}$", message = "AngelOne password must be 4-6 digits")
    private String angelOnePassword;

    private String angelOneTotpSecret;

    // Terms and conditions
    private boolean agreeToTerms;

    // Constructors
    public UserRegistrationDto() {
    }

    public UserRegistrationDto(String username, String email, String password, String confirmPassword,
                               String firstName, String lastName, String phoneNumber,
                               String angelOneApiKey, String angelOneClientId, String angelOnePassword,
                               String angelOneTotpSecret, boolean agreeToTerms) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.angelOneApiKey = angelOneApiKey;
        this.angelOneClientId = angelOneClientId;
        this.angelOnePassword = angelOnePassword;
        this.angelOneTotpSecret = angelOneTotpSecret;
        this.agreeToTerms = agreeToTerms;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAngelOneApiKey() {
        return angelOneApiKey;
    }

    public void setAngelOneApiKey(String angelOneApiKey) {
        this.angelOneApiKey = angelOneApiKey;
    }

    public String getAngelOneClientId() {
        return angelOneClientId;
    }

    public void setAngelOneClientId(String angelOneClientId) {
        this.angelOneClientId = angelOneClientId;
    }

    public String getAngelOnePassword() {
        return angelOnePassword;
    }

    public void setAngelOnePassword(String angelOnePassword) {
        this.angelOnePassword = angelOnePassword;
    }

    public String getAngelOneTotpSecret() {
        return angelOneTotpSecret;
    }

    public void setAngelOneTotpSecret(String angelOneTotpSecret) {
        this.angelOneTotpSecret = angelOneTotpSecret;
    }

    public boolean isAgreeToTerms() {
        return agreeToTerms;
    }

    public void setAgreeToTerms(boolean agreeToTerms) {
        this.agreeToTerms = agreeToTerms;
    }

    /**
     * Validate password confirmation.
     */
    public boolean isPasswordMatching() {
        return password != null && password.equals(confirmPassword);
    }
}
