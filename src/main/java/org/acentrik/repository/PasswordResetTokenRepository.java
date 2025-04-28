package org.acentrik.repository;

import org.acentrik.model.PasswordResetToken;
import org.acentrik.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for password reset tokens
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Find a token by its value
     * 
     * @param token The token value
     * @return The token entity if found
     */
    Optional<PasswordResetToken> findByToken(String token);
    
    /**
     * Find all tokens for a user
     * 
     * @param user The user
     * @return All tokens for the user
     */
    Iterable<PasswordResetToken> findByUser(User user);
    
    /**
     * Delete all expired tokens
     * 
     * @param now The current time
     * @return The number of tokens deleted
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiryDate < ?1")
    int deleteAllExpiredTokens(LocalDateTime now);
    
    /**
     * Delete all tokens for a user
     * 
     * @param user The user
     */
    void deleteByUser(User user);
}