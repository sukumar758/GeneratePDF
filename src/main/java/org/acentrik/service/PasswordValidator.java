package org.acentrik.service;

import org.acentrik.model.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service for validating password complexity and history
 */
@Component
public class PasswordValidator {

    // Password must be at least 8 characters long
    private static final int MIN_LENGTH = 8;
    
    // Password must contain at least one uppercase letter
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    
    // Password must contain at least one lowercase letter
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    
    // Password must contain at least one digit
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    
    // Password must contain at least one special character
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

    /**
     * Validates a password against complexity requirements
     * 
     * @param password The password to validate
     * @return A list of validation errors, empty if password is valid
     */
    public List<String> validatePasswordComplexity(String password) {
        List<String> errors = new ArrayList<>();
        
        if (password == null || password.length() < MIN_LENGTH) {
            errors.add("Password must be at least " + MIN_LENGTH + " characters long");
        }
        
        if (password != null && !UPPERCASE_PATTERN.matcher(password).matches()) {
            errors.add("Password must contain at least one uppercase letter");
        }
        
        if (password != null && !LOWERCASE_PATTERN.matcher(password).matches()) {
            errors.add("Password must contain at least one lowercase letter");
        }
        
        if (password != null && !DIGIT_PATTERN.matcher(password).matches()) {
            errors.add("Password must contain at least one digit");
        }
        
        if (password != null && !SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
            errors.add("Password must contain at least one special character");
        }
        
        return errors;
    }
    
    /**
     * Checks if a new password is in the user's password history
     * 
     * @param user The user
     * @param newPassword The new password (already encoded)
     * @return true if the password is in the history, false otherwise
     */
    public boolean isPasswordInHistory(User user, String newPassword) {
        if (user.getPasswordHistory() == null || user.getPasswordHistory().isEmpty()) {
            return false;
        }
        
        return user.getPasswordHistory().contains(newPassword);
    }
    
    /**
     * Generates a random password that meets complexity requirements
     * 
     * @return A random password
     */
    public String generateRandomPassword() {
        String upperCaseLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCaseLetters = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String specialCharacters = "!@#$%^&*()-_=+[]{}|;:,.<>?";
        
        StringBuilder password = new StringBuilder();
        
        // Ensure at least one character from each category
        password.append(upperCaseLetters.charAt((int) (Math.random() * upperCaseLetters.length())));
        password.append(lowerCaseLetters.charAt((int) (Math.random() * lowerCaseLetters.length())));
        password.append(numbers.charAt((int) (Math.random() * numbers.length())));
        password.append(specialCharacters.charAt((int) (Math.random() * specialCharacters.length())));
        
        // Add more random characters to reach minimum length
        String allCharacters = upperCaseLetters + lowerCaseLetters + numbers + specialCharacters;
        for (int i = 4; i < MIN_LENGTH; i++) {
            password.append(allCharacters.charAt((int) (Math.random() * allCharacters.length())));
        }
        
        // Shuffle the password characters
        char[] passwordArray = password.toString().toCharArray();
        for (int i = 0; i < passwordArray.length; i++) {
            int j = (int) (Math.random() * passwordArray.length);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }
        
        return new String(passwordArray);
    }
}