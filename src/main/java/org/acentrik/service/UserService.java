package org.acentrik.service;

import org.acentrik.model.PasswordResetToken;
import org.acentrik.model.User;
import org.acentrik.repository.PasswordResetTokenRepository;
import org.acentrik.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;

    @Value("${app.admin.auto-recreate:true}")
    private boolean adminAutoRecreate;

    @Value("${app.password-reset.token-expiry-minutes:30}")
    private int tokenExpiryMinutes;

    @Autowired
    public UserService(
            UserRepository userRepository, 
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder, 
            PasswordValidator passwordValidator) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidator = passwordValidator;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // Check if account is locked
        if (user.isAccountLocked()) {
            throw new LockedException("Account is locked due to too many failed login attempts");
        }

        // Check if password is expired
        boolean passwordExpired = user.isPasswordExpired();

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                true, // enabled
                true, // account not expired
                !passwordExpired, // credentials not expired
                !user.isAccountLocked(), // account not locked
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }

    @Transactional
    public void initializeUsers() {
        // Get credentials from environment variables or use defaults for development only
        String adminUsername = System.getenv("ADMIN_USERNAME") != null ? System.getenv("ADMIN_USERNAME") : "Admin";
        String adminPassword = System.getenv("ADMIN_PASSWORD") != null ? System.getenv("ADMIN_PASSWORD") : "Admin123!";
        String userUsername = System.getenv("USER_USERNAME") != null ? System.getenv("USER_USERNAME") : "User";
        String userPassword = System.getenv("USER_PASSWORD") != null ? System.getenv("USER_PASSWORD") : "User123!";

        // Ensure passwords meet complexity requirements
        List<String> adminPasswordErrors = passwordValidator.validatePasswordComplexity(adminPassword);
        if (!adminPasswordErrors.isEmpty()) {
            adminPassword = passwordValidator.generateRandomPassword();
            System.out.println("Generated secure admin password: " + adminPassword);
        }

        List<String> userPasswordErrors = passwordValidator.validatePasswordComplexity(userPassword);
        if (!userPasswordErrors.isEmpty()) {
            userPassword = passwordValidator.generateRandomPassword();
            System.out.println("Generated secure user password: " + userPassword);
        }

        // Check if admin user exists and recreate only if auto-recreate is enabled
        if (userRepository.findByUsername(adminUsername).isEmpty() && adminAutoRecreate) {
            User adminUser = new User(adminUsername, passwordEncoder.encode(adminPassword), "ADMIN");
            userRepository.save(adminUser);
        }

        // Check if regular user exists
        if (userRepository.findByUsername(userUsername).isEmpty()) {
            User regularUser = new User(userUsername, passwordEncoder.encode(userPassword), "USER");
            userRepository.save(regularUser);
        }
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Register a new user with password validation
     * 
     * @param username The username
     * @param password The password (plain text)
     * @return The created user
     * @throws IllegalArgumentException if username exists or password doesn't meet complexity requirements
     */
    @Transactional
    public User registerNewUser(String username, String password) {
        // Check if username already exists
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Validate password complexity
        List<String> validationErrors = passwordValidator.validatePasswordComplexity(password);
        if (!validationErrors.isEmpty()) {
            throw new IllegalArgumentException("Password does not meet complexity requirements: " + 
                    String.join(", ", validationErrors));
        }

        // Create new user with USER role
        String encodedPassword = passwordEncoder.encode(password);
        User newUser = new User(username, encodedPassword, "USER");
        return userRepository.save(newUser);
    }

    /**
     * Update a user's password with validation
     * 
     * @param user The user to update
     * @param newPassword The new password (plain text)
     * @return The updated user
     * @throws IllegalArgumentException if password doesn't meet complexity requirements or is in history
     */
    @Transactional
    public User updatePassword(User user, String newPassword) {
        // Validate password complexity
        List<String> validationErrors = passwordValidator.validatePasswordComplexity(newPassword);
        if (!validationErrors.isEmpty()) {
            throw new IllegalArgumentException("Password does not meet complexity requirements: " + 
                    String.join(", ", validationErrors));
        }

        // Encode the new password
        String encodedPassword = passwordEncoder.encode(newPassword);

        // Check if the password is in the history
        if (passwordValidator.isPasswordInHistory(user, encodedPassword)) {
            throw new IllegalArgumentException("Password has been used recently. Please choose a different password.");
        }

        // Add the current password to history before updating
        user.addPasswordToHistory(user.getPassword());

        // Update the password and reset expiration
        user.setPassword(encodedPassword);
        user.setLastPasswordChange(LocalDateTime.now());
        user.setPasswordExpiryDate(LocalDateTime.now().plusDays(90)); // Passwords expire after 90 days

        return userRepository.save(user);
    }

    /**
     * Delete a user by ID
     * 
     * @param userId The ID of the user to delete
     * @return true if the user was deleted, false otherwise
     */
    public boolean deleteUserById(Long userId) {
        if (userRepository.existsById(userId)) {
            userRepository.deleteById(userId);
            return true;
        }
        return false;
    }

    /**
     * Create or recreate the admin user
     * This method can be used to recover the admin account if it was deleted by mistake
     * 
     * @return The created admin user
     */
    @Transactional
    public User createAdminUser() {
        // Get credentials from environment variables or use defaults
        String adminUsername = System.getenv("ADMIN_USERNAME") != null ? System.getenv("ADMIN_USERNAME") : "Admin";
        String adminPassword = System.getenv("ADMIN_PASSWORD") != null ? System.getenv("ADMIN_PASSWORD") : "Admin123!";

        // Check if admin user already exists
        Optional<User> existingAdmin = userRepository.findByUsername(adminUsername);
        if (existingAdmin.isPresent()) {
            return existingAdmin.get();
        }

        // Ensure the default admin password meets complexity requirements
        // If not, generate a secure random password
        List<String> validationErrors = passwordValidator.validatePasswordComplexity(adminPassword);
        if (!validationErrors.isEmpty()) {
            adminPassword = passwordValidator.generateRandomPassword();
            System.out.println("Generated secure admin password: " + adminPassword);
        }

        // Create new admin user
        String encodedPassword = passwordEncoder.encode(adminPassword);
        User adminUser = new User(adminUsername, encodedPassword, "ADMIN");
        return userRepository.save(adminUser);
    }

    /**
     * Record a successful login for a user
     * Resets failed login attempts and account lock
     * 
     * @param username The username
     */
    @Transactional
    public void recordSuccessfulLogin(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.resetFailedLoginAttempts();
            userRepository.save(user);
        });
    }

    /**
     * Record a failed login attempt for a user
     * Increments failed login attempts and may lock the account
     * 
     * @param username The username
     * @return true if the account is now locked, false otherwise
     */
    @Transactional
    public boolean recordFailedLogin(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.incrementFailedLoginAttempts();
            userRepository.save(user);
            return user.isAccountLocked();
        }
        return false;
    }

    /**
     * Check if a user's password is expired
     * 
     * @param username The username
     * @return true if the password is expired, false otherwise
     */
    public boolean isPasswordExpired(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        return userOpt.map(User::isPasswordExpired).orElse(false);
    }

    /**
     * Check if a user's account is locked
     * 
     * @param username The username
     * @return true if the account is locked, false otherwise
     */
    public boolean isAccountLocked(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        return userOpt.map(User::isAccountLocked).orElse(false);
    }

    /**
     * Create a password reset token for a user
     * 
     * @param username The username
     * @return The token string or null if user not found
     */
    @Transactional
    public String createPasswordResetToken(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return null;
        }

        User user = userOpt.get();

        // Delete any existing tokens for this user
        tokenRepository.deleteByUser(user);

        // Create a new token
        PasswordResetToken token = new PasswordResetToken(user, tokenExpiryMinutes);
        tokenRepository.save(token);

        return token.getToken();
    }

    /**
     * Validate a password reset token
     * 
     * @param token The token string
     * @return The user if the token is valid, null otherwise
     */
    @Transactional(readOnly = true)
    public User validatePasswordResetToken(String token) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            return null;
        }

        PasswordResetToken resetToken = tokenOpt.get();

        // Check if token is expired
        if (resetToken.isExpired()) {
            return null;
        }

        return resetToken.getUser();
    }

    /**
     * Reset a user's password using a token
     * 
     * @param token The token string
     * @param newPassword The new password
     * @return true if password was reset, false otherwise
     */
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        User user = validatePasswordResetToken(token);

        if (user == null) {
            return false;
        }

        // Validate password complexity
        List<String> validationErrors = passwordValidator.validatePasswordComplexity(newPassword);
        if (!validationErrors.isEmpty()) {
            throw new IllegalArgumentException("Password does not meet complexity requirements: " + 
                    String.join(", ", validationErrors));
        }

        // Update the password
        user.addPasswordToHistory(user.getPassword());
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setLastPasswordChange(LocalDateTime.now());
        user.setPasswordExpiryDate(LocalDateTime.now().plusDays(90));
        userRepository.save(user);

        // Delete the used token
        tokenRepository.deleteByUser(user);

        return true;
    }

    /**
     * Clean up expired tokens
     * Should be called periodically by a scheduled task
     */
    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepository.deleteAllExpiredTokens(LocalDateTime.now());
    }
}
