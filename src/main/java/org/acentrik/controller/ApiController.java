
package org.acentrik.controller;

import org.acentrik.model.User;
import org.acentrik.service.UserService;
import org.acentrik.service.OfferLetterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class ApiController {

    @Autowired
    private UserService userService;

    @Autowired
    private OfferLetterService offerLetterService;

    @GetMapping("/auth/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("username", auth.getName());
            userInfo.put("role", auth.getAuthorities().iterator().next().getAuthority());
            userInfo.put("authenticated", true);
            return ResponseEntity.ok(userInfo);
        }
        return ResponseEntity.ok(Map.of("authenticated", false));
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.ok(userService.getAllUsers());
        }
        return ResponseEntity.status(403).build();
    }

    @PostMapping("/users")
    public ResponseEntity<Map<String, String>> createUser(@RequestBody Map<String, String> userData) {
        try {
            String username = userData.get("username");
            String password = userData.get("password");
            userService.registerNewUser(username, password);
            return ResponseEntity.ok(Map.of("message", "User created successfully", "username", username));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "An error occurred during registration"));
        }
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            try {
                boolean deleted = userService.deleteUserById(userId);
                if (deleted) {
                    return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
                } else {
                    return ResponseEntity.notFound().build();
                }
            } catch (Exception e) {
                return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
            }
        }
        return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getUserProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            String username = auth.getName();
            Optional<User> userOpt = userService.getUserByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                Map<String, Object> profile = new HashMap<>();
                profile.put("id", user.getId());
                profile.put("username", user.getUsername());
                profile.put("role", user.getRole());
                profile.put("hasOfferLetter", offerLetterService.getLatestOfferLetterForUser(user).isPresent());
                return ResponseEntity.ok(profile);
            }
        }
        return ResponseEntity.status(401).build();
    }

    @PutMapping("/profile/password")
    public ResponseEntity<Map<String, String>> updatePassword(@RequestBody Map<String, String> passwordData) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            try {
                String newPassword = passwordData.get("newPassword");
                String confirmPassword = passwordData.get("confirmPassword");
                
                if (!newPassword.equals(confirmPassword)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Passwords do not match"));
                }

                Optional<User> userOpt = userService.getUserByUsername(auth.getName());
                if (userOpt.isPresent()) {
                    userService.updatePassword(userOpt.get(), newPassword);
                    return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
                }
                return ResponseEntity.notFound().build();
            } catch (Exception e) {
                return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
            }
        }
        return ResponseEntity.status(401).build();
    }
}
