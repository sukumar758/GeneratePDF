package org.acentrik.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;

    @Column(name = "failed_login_attempts")
    private int failedLoginAttempts = 0;

    @Column(name = "account_locked")
    private boolean accountLocked = false;

    @Column(name = "last_password_change")
    private LocalDateTime lastPasswordChange;

    @Column(name = "password_expiry_date")
    private LocalDateTime passwordExpiryDate;

    @ElementCollection
    @CollectionTable(name = "password_history", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "password")
    private List<String> passwordHistory = new ArrayList<>();

    // Constructor with username, password, and role
    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.lastPasswordChange = LocalDateTime.now();
        this.passwordExpiryDate = LocalDateTime.now().plusDays(90); // Default 90 days expiry
    }

    /**
     * Check if the account is locked due to too many failed login attempts
     * @return true if the account is locked
     */
    public boolean isAccountLocked() {
        return accountLocked;
    }

    /**
     * Check if the password has expired
     * @return true if the password has expired
     */
    public boolean isPasswordExpired() {
        return passwordExpiryDate != null && LocalDateTime.now().isAfter(passwordExpiryDate);
    }

    /**
     * Add a password to the history
     * @param password The password to add
     */
    public void addPasswordToHistory(String password) {
        if (passwordHistory == null) {
            passwordHistory = new ArrayList<>();
        }
        passwordHistory.add(password);
        // Keep only the last 5 passwords
        if (passwordHistory.size() > 5) {
            passwordHistory.remove(0);
        }
    }

    /**
     * Reset failed login attempts counter
     */
    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.accountLocked = false;
    }

    /**
     * Increment failed login attempts and lock account if threshold is reached
     */
    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
        // Lock account after 5 failed attempts
        if (this.failedLoginAttempts >= 5) {
            this.accountLocked = true;
        }
    }
}
