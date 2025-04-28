package org.acentrik.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for auditing sensitive operations
 */
@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger("security-audit");
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Log an authentication event
     * 
     * @param username The username
     * @param eventType The event type (e.g., "LOGIN_SUCCESS", "LOGIN_FAILURE")
     * @param details Additional details about the event
     */
    public void logAuthEvent(String username, String eventType, String details) {
        String timestamp = LocalDateTime.now().format(formatter);
        String message = String.format("[%s] AUTH_EVENT: %s - User: %s - %s", 
                timestamp, eventType, username, details);
        logger.info(message);
    }

    /**
     * Log a password change event
     * 
     * @param username The username
     * @param eventType The event type (e.g., "PASSWORD_CHANGE", "PASSWORD_RESET")
     * @param details Additional details about the event
     */
    public void logPasswordEvent(String username, String eventType, String details) {
        String timestamp = LocalDateTime.now().format(formatter);
        String message = String.format("[%s] PASSWORD_EVENT: %s - User: %s - %s", 
                timestamp, eventType, username, details);
        logger.info(message);
    }

    /**
     * Log an offer letter event
     * 
     * @param username The username
     * @param eventType The event type (e.g., "GENERATE", "VIEW", "DOWNLOAD")
     * @param offerId The offer letter ID
     * @param details Additional details about the event
     */
    public void logOfferLetterEvent(String username, String eventType, Long offerId, String details) {
        String timestamp = LocalDateTime.now().format(formatter);
        String message = String.format("[%s] OFFER_LETTER_EVENT: %s - User: %s - OfferID: %d - %s", 
                timestamp, eventType, username, offerId, details);
        logger.info(message);
    }

    /**
     * Log a user management event
     * 
     * @param adminUsername The admin username
     * @param eventType The event type (e.g., "CREATE_USER", "DELETE_USER")
     * @param targetUsername The target username
     * @param details Additional details about the event
     */
    public void logUserManagementEvent(String adminUsername, String eventType, String targetUsername, String details) {
        String timestamp = LocalDateTime.now().format(formatter);
        String message = String.format("[%s] USER_MANAGEMENT_EVENT: %s - Admin: %s - Target: %s - %s", 
                timestamp, eventType, adminUsername, targetUsername, details);
        logger.info(message);
    }
}