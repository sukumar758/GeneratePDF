package org.acentrik.config;

import org.acentrik.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationFailureLockedEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Custom authentication event publisher that tracks login attempts
 * and locks accounts after too many failed attempts
 */
@Component
public class CustomAuthenticationEventPublisher implements AuthenticationEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final UserService userService;

    @Autowired
    public CustomAuthenticationEventPublisher(
            ApplicationEventPublisher applicationEventPublisher,
            UserService userService) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.userService = userService;
    }

    @Override
    public void publishAuthenticationSuccess(Authentication authentication) {
        // Record successful login to reset failed attempts counter
        String username = authentication.getName();
        userService.recordSuccessfulLogin(username);
        
        // Publish the standard success event
        applicationEventPublisher.publishEvent(new AuthenticationSuccessEvent(authentication));
    }

    @Override
    public void publishAuthenticationFailure(AuthenticationException exception, Authentication authentication) {
        String username = authentication.getName();
        
        // Handle different types of authentication failures
        if (exception instanceof BadCredentialsException) {
            // Record failed login attempt
            boolean isLocked = userService.recordFailedLogin(username);
            
            if (isLocked) {
                // If account is now locked, publish a locked event
                LockedException lockedException = new LockedException(
                        "Account locked due to too many failed login attempts");
                applicationEventPublisher.publishEvent(
                        new AuthenticationFailureLockedEvent(authentication, lockedException));
            } else {
                // Otherwise publish a bad credentials event
                applicationEventPublisher.publishEvent(
                        new AuthenticationFailureBadCredentialsEvent(authentication, exception));
            }
        } else {
            // For other types of failures, just publish the event
            if (exception instanceof LockedException) {
                applicationEventPublisher.publishEvent(
                        new AuthenticationFailureLockedEvent(authentication, exception));
            } else {
                // Default handling for other exception types
                applicationEventPublisher.publishEvent(
                        new AuthenticationFailureBadCredentialsEvent(authentication, exception));
            }
        }
    }
}