package org.acentrik.controller;

import org.acentrik.model.User;
import org.acentrik.service.UserService;
import org.acentrik.service.OfferLetterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
public class PageController {

    @Autowired
    private UserService userService;

    @Autowired
    private OfferLetterService offerLetterService;

    @GetMapping("/")
    public String index(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            model.addAttribute("username", auth.getName());
            model.addAttribute("role", auth.getAuthorities().iterator().next().getAuthority());

            // If user is admin, get all users for quick access
            if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                List<User> users = userService.getAllUsers();
                model.addAttribute("users", users);
            }
        }
        return "index"; // Loads index.html
    }

    @GetMapping("/form")
    public String formPage(@RequestParam(required = false) String username, Model model) {
        if (username != null && !username.isEmpty()) {
            // If username is provided, fetch user details and add to model
            userService.getUserByUsername(username).ifPresent(user -> {
                model.addAttribute("username", user.getUsername());
            });
        }
        return "form"; // Loads form.html
    }

    @GetMapping("/eform")
    public String emailOLPage() {
        return "eform"; // Your email sending form page
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login"; // Loads login.html
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register"; // Loads register.html
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String username, 
                              @RequestParam String password,
                              @RequestParam String confirmPassword,
                              RedirectAttributes redirectAttributes) {
        // Validate password confirmation
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match");
            return "redirect:/register";
        }

        try {
            // Register new user
            userService.registerNewUser(username, password);
            redirectAttributes.addFlashAttribute("success", "Registration successful! You can now login.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An error occurred during registration");
            return "redirect:/register";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, jakarta.servlet.http.HttpSession session) {
        // Get the current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            model.addAttribute("username", auth.getName());
            model.addAttribute("role", auth.getAuthorities().iterator().next().getAuthority());

            // If user is admin, get all users and their offer letters
            if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                List<User> users = userService.getAllUsers();
                model.addAttribute("users", users);

                // Create a map to store offer letters by user ID
                java.util.Map<Long, org.acentrik.model.OfferLetter> offerLetterMap = new java.util.HashMap<>();

                // Fetch offer letters for each user
                for (User user : users) {
                    offerLetterService.getLatestOfferLetterForUser(user).ifPresent(offerLetter -> {
                        offerLetterMap.put(user.getId(), offerLetter);
                    });
                }

                // Add the map to the model
                model.addAttribute("offerLetterMap", offerLetterMap);

                // Check if an offer letter was just downloaded
                Boolean offerLetterDownloaded = (Boolean) session.getAttribute("offerLetterDownloaded");
                if (offerLetterDownloaded != null && offerLetterDownloaded) {
                    model.addAttribute("successMessage", "Offer letter was successfully downloaded and saved to the system.");
                    // Remove the session attribute to prevent showing the message again on refresh
                    session.removeAttribute("offerLetterDownloaded");
                }
            }
        }
        return "dashboard";
    }

    @PostMapping("/addEmployee")
    public String addEmployee(@RequestParam String username, 
                             @RequestParam String password,
                             RedirectAttributes redirectAttributes) {
        try {
            // Register new user with USER role
            userService.registerNewUser(username, password);
            redirectAttributes.addFlashAttribute("successMessage", "Employee added successfully: " + username);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred: " + e.getMessage());
        }

        return "redirect:/dashboard";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        // Get the current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            String username = auth.getName();
            model.addAttribute("username", username);
            model.addAttribute("role", auth.getAuthorities().iterator().next().getAuthority());

            // Get user details
            userService.getUserByUsername(username).ifPresent(user -> {
                model.addAttribute("user", user);

                // Check if user has an offer letter
                boolean hasOfferLetter = offerLetterService.getLatestOfferLetterForUser(user).isPresent();
                model.addAttribute("offerLetter", hasOfferLetter ? "available" : null);
            });
        }
        return "profile";
    }

    @PostMapping("/updateProfile")
    public String updateProfile(@RequestParam String username, 
                               @RequestParam(required = false) String newPassword,
                               @RequestParam(required = false) String confirmPassword,
                               RedirectAttributes redirectAttributes) {
        // Get the current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            // Verify the username matches the current user
            if (!auth.getName().equals(username)) {
                redirectAttributes.addFlashAttribute("error", "You can only update your own profile");
                return "redirect:/profile";
            }

            // If password is provided, update it
            if (newPassword != null && !newPassword.isEmpty()) {
                // Validate password confirmation
                if (!newPassword.equals(confirmPassword)) {
                    redirectAttributes.addFlashAttribute("error", "Passwords do not match");
                    return "redirect:/profile";
                }

                try {
                    // Get the user and update password
                    Optional<User> userOpt = userService.getUserByUsername(username);
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        userService.updatePassword(user, newPassword);
                        redirectAttributes.addFlashAttribute("success", "Password updated successfully");
                    } else {
                        redirectAttributes.addFlashAttribute("error", "User not found");
                    }
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("error", "An error occurred: " + e.getMessage());
                }
            } else {
                redirectAttributes.addFlashAttribute("info", "No changes were made");
            }
        }

        return "redirect:/profile";
    }

    @PostMapping("/terminateEmployee")
    public String terminateEmployee(@RequestParam Long userId, RedirectAttributes redirectAttributes) {
        // Check if the current user is an admin
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && 
            auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {

            try {
                // Attempt to delete the user
                boolean deleted = userService.deleteUserById(userId);
                if (deleted) {
                    redirectAttributes.addFlashAttribute("successMessage", "Employee terminated successfully");
                } else {
                    redirectAttributes.addFlashAttribute("errorMessage", "Employee not found");
                }
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "An error occurred: " + e.getMessage());
            }
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "You do not have permission to terminate employees");
        }

        return "redirect:/dashboard";
    }

    /**
     * Endpoint to recreate the admin account if it was deleted by mistake.
     * This is a safety measure and should be used with caution.
     */
    @GetMapping("/recreateAdmin")
    public String recreateAdminForm() {
        return "recreateAdmin"; // This would be a simple form with a button to recreate the admin
    }

    /**
     * Process the admin recreation request
     */
    @PostMapping("/recreateAdmin")
    public String recreateAdmin(RedirectAttributes redirectAttributes) {
        try {
            // Create or get the admin user
            User adminUser = userService.createAdminUser();

            redirectAttributes.addFlashAttribute("successMessage", 
                "Admin account has been recreated successfully. You can now login with the default credentials.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Failed to recreate admin account: " + e.getMessage());
            return "redirect:/recreateAdmin";
        }
    }
}
