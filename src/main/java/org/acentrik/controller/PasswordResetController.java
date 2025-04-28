package org.acentrik.controller;

import org.acentrik.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for password reset functionality
 */
@Controller
public class PasswordResetController {

    private final UserService userService;

    @Autowired
    public PasswordResetController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Display the forgot password form
     */
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgot-password";
    }

    /**
     * Process the forgot password form submission
     * Creates a password reset token and sends it to the user's email
     */
    @PostMapping("/forgot-password")
    public String processForgotPassword(
            @RequestParam("username") String username,
            RedirectAttributes redirectAttributes) {
        
        String token = userService.createPasswordResetToken(username);
        
        if (token == null) {
            // Don't reveal if the user exists or not for security reasons
            redirectAttributes.addFlashAttribute("message", 
                    "If your account exists, a password reset link has been sent to your email.");
            return "redirect:/login";
        }
        
        // In a real application, you would send an email with the reset link
        // For this example, we'll just display the token
        String resetUrl = "/reset-password?token=" + token;
        redirectAttributes.addFlashAttribute("message", 
                "Password reset link: " + resetUrl);
        
        return "redirect:/login";
    }

    /**
     * Display the reset password form
     */
    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("token") String token, Model model) {
        // Validate the token
        if (userService.validatePasswordResetToken(token) == null) {
            model.addAttribute("error", "Invalid or expired password reset token.");
            return "error";
        }
        
        model.addAttribute("token", token);
        return "reset-password";
    }

    /**
     * Process the reset password form submission
     */
    @PostMapping("/reset-password")
    public String processResetPassword(
            @RequestParam("token") String token,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {
        
        // Check if passwords match
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match.");
            return "redirect:/reset-password?token=" + token;
        }
        
        try {
            boolean result = userService.resetPassword(token, password);
            
            if (!result) {
                redirectAttributes.addFlashAttribute("error", "Invalid or expired password reset token.");
                return "redirect:/forgot-password";
            }
            
            redirectAttributes.addFlashAttribute("message", 
                    "Your password has been reset successfully. You can now log in with your new password.");
            return "redirect:/login";
            
        } catch (IllegalArgumentException e) {
            // Password doesn't meet complexity requirements
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/reset-password?token=" + token;
        }
    }
}