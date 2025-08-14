package org.acentrik.controller;


import com.itextpdf.io.exceptions.IOException;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.element.Image;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.layout.properties.TextAlignment;
import jakarta.servlet.http.HttpServletResponse;
import org.acentrik.model.FormData;
import org.acentrik.model.OfferLetter;
import org.acentrik.model.User;
import org.acentrik.service.EmailService;
import org.acentrik.service.OfferLetterService;
import org.acentrik.service.UserService;
import org.acentrik.service.PasswordValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Controller
public class PdfController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserService userService;

    @Autowired
    private OfferLetterService offerLetterService;

    @Autowired
    private PasswordValidator passwordValidator;

    @PostMapping("/generatePdf")
    public void downloadPDF(@ModelAttribute FormData formData, HttpServletResponse response, jakarta.servlet.http.HttpSession session) throws java.io.IOException {
        try {
            response.setContentType("application/pdf");
            String fileName = formData.getFirstName() + formData.getLastName() + ".pdf";
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            // Ensure no caching to prevent issues with PDF download
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");

            // Set a session attribute to indicate that an offer letter was just downloaded
            session.setAttribute("offerLetterDownloaded", true);

            // Get current admin user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String adminUsername = auth.getName();

            // Generate PDF
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            generatePdfDocument(formData, baos);
            byte[] pdfBytes = baos.toByteArray();

            // Create a new user account for the employee if it doesn't exist
            String employeeEmail = formData.getEmail();
            User employeeUser = null;

            try {
                Optional<User> existingUser = userService.getUserByUsername(employeeEmail);
                if (existingUser.isPresent()) {
                    employeeUser = existingUser.get();
                    System.out.println("Using existing user account for: " + employeeEmail);
                } else {
                    // Generate a random password
                    String generatedPassword = generateRandomPassword();

                    // Create new user with USER role
                    employeeUser = userService.registerNewUser(employeeEmail, generatedPassword);
                    System.out.println("Created new user account for: " + employeeEmail + " with password: " + generatedPassword);
                    // In a production environment, you would email these credentials to the user
                }
            } catch (Exception e) {
                System.err.println("Failed to create/retrieve user account: " + e.getMessage());
                e.printStackTrace();
            }

            // Store PDF for the employee user
            if (employeeUser != null) {
                try {
                    offerLetterService.saveOfferLetter(employeeUser, fileName, pdfBytes);
                    System.out.println("Saved offer letter for employee: " + employeeEmail);
                } catch (Exception e) {
                    System.err.println("Failed to save offer letter for employee: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // No need to store offer letters for admin users
            // Admin only manages offer letters for employees

            // Write PDF to response
            try (OutputStream out = response.getOutputStream()) {
                out.write(pdfBytes);
                System.out.println("PDF successfully written to response");
            }
        } catch (Exception e) {
            System.err.println("Error in downloadPDF: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to let Spring handle the error response
        }
    }

    /**
     * Generates a random password for new user accounts
     * @return A random password
     */
    private String generateRandomPassword() {
        return passwordValidator.generateRandomPassword();
    }
    @PostMapping("/emailPdf")
    public String emailPDF(@ModelAttribute FormData formData, Model model) {
        try {
            // 1. Generate PDF in memory
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                generatePdfDocument(formData, baos);
                System.out.println("PDF document generated successfully");
            } catch (Exception e) {
                System.err.println("Failed to generate PDF document: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Failed to generate PDF document", e);
            }
            byte[] pdfBytes = baos.toByteArray();

            // 2. Prepare email content
            String fileName = formData.getFirstName() + formData.getLastName() + "_OfferLetter.pdf";
            String subject = "Your Internship Offer from Acentrik Technology Solutions";
            String body = buildEmailBody(formData);

            // 3. Create a new user account for the employee if it doesn't exist
            String employeeEmail = formData.getEmail();
            User employeeUser = null;
            String generatedPassword = null;

            try {
                Optional<User> existingUser = userService.getUserByUsername(employeeEmail);
                if (existingUser.isPresent()) {
                    employeeUser = existingUser.get();
                    System.out.println("Using existing user account for: " + employeeEmail);
                } else {
                    // Generate a random password
                    generatedPassword = generateRandomPassword();

                    // Create new user with USER role
                    employeeUser = userService.registerNewUser(employeeEmail, generatedPassword);
                    System.out.println("Created new user account for: " + employeeEmail + " with password: " + generatedPassword);
                }
            } catch (Exception e) {
                System.err.println("Failed to create/retrieve user account: " + e.getMessage());
                e.printStackTrace();
                // Continue with email sending even if user creation fails
            }

            // 4. Add login credentials to the email if a new account was created
            if (generatedPassword != null) {
                body += "\n\n----- YOUR LOGIN CREDENTIALS -----\n";
                body += "Username: " + employeeEmail + "\n";
                body += "Password: " + generatedPassword + "\n";
                body += "Please login at our portal to view your profile and offer letter.\n";
            }

            // 5. Send email
            try {
                emailService.sendPdfEmail(
                        employeeEmail,
                        subject,
                        body,
                        pdfBytes,
                        fileName
                );
                System.out.println("Email sent successfully to: " + employeeEmail);
            } catch (Exception e) {
                System.err.println("Failed to send email: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Failed to send email", e);
            }

            // 6. Store PDF for the employee user
            if (employeeUser != null) {
                try {
                    offerLetterService.saveOfferLetter(employeeUser, fileName, pdfBytes);
                    System.out.println("Saved offer letter for employee: " + employeeEmail);
                } catch (Exception e) {
                    System.err.println("Failed to save offer letter for employee: " + e.getMessage());
                    e.printStackTrace();
                    // Continue even if saving fails
                }
            }

            // 7. No need to store offer letters for admin users
            // Admin only manages offer letters for employees

            // 8. Return success view
            model.addAttribute("recipientEmail", employeeEmail);
            return "email-success";

        } catch (Exception e) {
            System.err.println("Error in emailPDF: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Failed to process request: " + e.getMessage());
            return "email-error";
        }
    }

    /**
     * Builds the email body text using the form data
     * 
     * @param formData The form data submitted by the user
     * @return Formatted email body text
     */
    private String buildEmailBody(FormData formData) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy");
        String formattedJoiningDate = formData.getJoiningDate().format(formatter);

        return "Dear " + formData.getFirstName() + " " + formData.getLastName() + ",\n\n" +
               "We are pleased to offer you the " + formData.getRole() + " position at Acentrik Technology Solutions. " +
               "Your internship will begin on " + formattedJoiningDate + ".\n\n" +
               "Please find attached your official offer letter with all the details.\n\n" +
               "If you have any questions, please don't hesitate to contact us.\n\n" +
               "Best regards,\n" +
               "Kishore Medikonda\n" +
               "HR Director\n" +
               "Acentrik Technology Solutions LLC";
    }

    @GetMapping("/viewOfferLetter")
    public void viewOfferLetter(HttpServletResponse response) throws java.io.IOException {
        // Get current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        if (!username.equals("anonymousUser")) {
            Optional<User> userOpt = userService.getUserByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                Optional<OfferLetter> offerLetterOpt = offerLetterService.getLatestOfferLetterForUser(user);

                if (offerLetterOpt.isPresent()) {
                    OfferLetter offerLetter = offerLetterOpt.get();

                    // Set response headers
                    response.setContentType("application/pdf");
                    response.setHeader("Content-Disposition", "inline; filename=\"" + offerLetter.getFileName() + "\"");

                    // Write PDF to response
                    try (OutputStream out = response.getOutputStream()) {
                        out.write(offerLetter.getContent());
                    }
                    return;
                }
            }
        }

        // If no offer letter found, redirect to profile page
        response.sendRedirect("/profile");
    }

    /**
     * View a specific offer letter by ID
     * 
     * @param id The ID of the offer letter to view
     * @param response The HTTP response
     * @throws java.io.IOException If an I/O error occurs
     */
    @GetMapping("/viewOfferLetter/{id}")
    public void viewOfferLetterById(@PathVariable Long id, HttpServletResponse response) throws java.io.IOException {
        // Get current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        if (!username.equals("anonymousUser")) {
            Optional<User> userOpt = userService.getUserByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                Optional<OfferLetter> offerLetterOpt = offerLetterService.getOfferLetterById(id);

                if (offerLetterOpt.isPresent()) {
                    OfferLetter offerLetter = offerLetterOpt.get();

                    // Check if the user has permission to view this offer letter
                    if (offerLetter.getUser().getId().equals(user.getId()) || user.getRole().equals("ADMIN")) {
                        // Set response headers
                        response.setContentType("application/pdf");
                        response.setHeader("Content-Disposition", "inline; filename=\"" + offerLetter.getFileName() + "\"");

                        // Write PDF to response
                        try (OutputStream out = response.getOutputStream()) {
                            out.write(offerLetter.getContent());
                        }
                        return;
                    }
                }
            }
        }

        // If no offer letter found or user doesn't have permission, redirect to profile page
        response.sendRedirect("/profile");
    }

    @GetMapping("/downloadOfferLetter")
    public void downloadOfferLetter(HttpServletResponse response) throws java.io.IOException {
        // Get current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        if (!username.equals("anonymousUser")) {
            Optional<User> userOpt = userService.getUserByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                Optional<OfferLetter> offerLetterOpt = offerLetterService.getLatestOfferLetterForUser(user);

                if (offerLetterOpt.isPresent()) {
                    OfferLetter offerLetter = offerLetterOpt.get();

                    // Set response headers
                    response.setContentType("application/pdf");
                    response.setHeader("Content-Disposition", "attachment; filename=\"" + offerLetter.getFileName() + "\"");

                    // Write PDF to response
                    try (OutputStream out = response.getOutputStream()) {
                        out.write(offerLetter.getContent());
                    }
                    return;
                }
            }
        }

        // If no offer letter found, redirect to profile page
        response.sendRedirect("/profile");
    }

    /**
     * Download a specific offer letter by ID
     * 
     * @param id The ID of the offer letter to download
     * @param response The HTTP response
     * @throws java.io.IOException If an I/O error occurs
     */
    @GetMapping("/downloadOfferLetter/{id}")
    public void downloadOfferLetterById(@PathVariable Long id, HttpServletResponse response) throws java.io.IOException {
        // Get current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        if (!username.equals("anonymousUser")) {
            Optional<User> userOpt = userService.getUserByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                Optional<OfferLetter> offerLetterOpt = offerLetterService.getOfferLetterById(id);

                if (offerLetterOpt.isPresent()) {
                    OfferLetter offerLetter = offerLetterOpt.get();

                    // Check if the user has permission to download this offer letter
                    if (offerLetter.getUser().getId().equals(user.getId()) || user.getRole().equals("ADMIN")) {
                        // Set response headers
                        response.setContentType("application/pdf");
                        response.setHeader("Content-Disposition", "attachment; filename=\"" + offerLetter.getFileName() + "\"");

                        // Write PDF to response
                        try (OutputStream out = response.getOutputStream()) {
                            out.write(offerLetter.getContent());
                        }
                        return;
                    }
                }
            }
        }

        // If no offer letter found or user doesn't have permission, redirect to profile page
        response.sendRedirect("/profile");
    }

    /**
     * Remove an offer letter by its ID
     * 
     * @param id The ID of the offer letter to remove
     * @return ResponseEntity with success/failure message
     */
    @DeleteMapping("/removeOfferLetter/{id}")
    public ResponseEntity<String> removeOfferLetter(@PathVariable Long id) {
        try {
            // Get current user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();

            if (username.equals("anonymousUser")) {
                return ResponseEntity.status(401).body("You must be logged in to remove an offer letter");
            }

            // Check if the offer letter exists
            Optional<OfferLetter> offerLetterOpt = offerLetterService.getOfferLetterById(id);
            if (!offerLetterOpt.isPresent()) {
                return ResponseEntity.status(404).body("Offer letter not found");
            }

            // Check if the user has permission to remove this offer letter
            OfferLetter offerLetter = offerLetterOpt.get();
            Optional<User> userOpt = userService.getUserByUsername(username);

            if (!userOpt.isPresent()) {
                return ResponseEntity.status(401).body("User not found");
            }

            User user = userOpt.get();

            // Only allow users to remove their own offer letters or admins to remove any offer letter
            if (!offerLetter.getUser().getId().equals(user.getId()) && !user.getRole().equals("ADMIN")) {
                return ResponseEntity.status(403).body("You don't have permission to remove this offer letter");
            }

            // Remove the offer letter
            boolean removed = offerLetterService.removeOfferLetterById(id);

            if (removed) {
                return ResponseEntity.ok("Offer letter removed successfully");
            } else {
                return ResponseEntity.status(500).body("Failed to remove offer letter");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Terminate an employee (remove user account)
     * Only accessible to admin users
     * 
     * @param userId The ID of the user to terminate
     * @return ResponseEntity with success/failure message
     */
    @DeleteMapping("/terminateEmployee/{userId}")
    public ResponseEntity<String> terminateEmployee(@PathVariable Long userId) {
        try {
            // Get current user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();

            if (username.equals("anonymousUser")) {
                return ResponseEntity.status(401).body("You must be logged in to terminate an employee");
            }

            // Check if the current user is an admin
            Optional<User> adminOpt = userService.getUserByUsername(username);
            if (!adminOpt.isPresent() || !adminOpt.get().getRole().equals("ADMIN")) {
                return ResponseEntity.status(403).body("Only administrators can terminate employees");
            }

            // Check if the user to terminate exists
            Optional<User> userToTerminateOpt = userService.getUserByUsername(userId.toString());
            if (!userToTerminateOpt.isPresent()) {
                return ResponseEntity.status(404).body("Employee not found");
            }

            User userToTerminate = userToTerminateOpt.get();

            // Don't allow terminating admin users
            if (userToTerminate.getRole().equals("ADMIN")) {
                return ResponseEntity.status(403).body("Cannot terminate administrator accounts");
            }

            // Delete the user
            boolean deleted = userService.deleteUserById(userId);

            if (deleted) {
                return ResponseEntity.ok("Employee terminated successfully");
            } else {
                return ResponseEntity.status(500).body("Failed to terminate employee");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Get all offer letters for the current user
     * If the user is an admin, get all offer letters for all users
     * 
     * @param model The model to add attributes to
     * @return The view name
     */
    @GetMapping("/myOfferLetters")
    public String getMyOfferLetters(Model model) {
        // Get current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        if (!username.equals("anonymousUser")) {
            Optional<User> userOpt = userService.getUserByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();

                // Check if user is admin
                boolean isAdmin = user.getRole().equals("ADMIN");
                model.addAttribute("isAdmin", isAdmin);

                if (isAdmin) {
                    // For admin users, get all offer letters for all users
                    List<User> allUsers = userService.getAllUsers();
                    // Filter out admin users
                    List<User> employees = allUsers.stream()
                        .filter(u -> !u.getRole().equals("ADMIN"))
                        .toList();

                    // Create a map of users to their offer letters
                    java.util.Map<User, List<OfferLetter>> employeeOfferLetters = new java.util.HashMap<>();
                    for (User employee : employees) {
                        List<OfferLetter> letters = offerLetterService.getOfferLettersForUser(employee);
                        if (!letters.isEmpty()) {
                            employeeOfferLetters.put(employee, letters);
                        }
                    }

                    model.addAttribute("employeeOfferLetters", employeeOfferLetters);
                } else {
                    // For regular users, get only their offer letters
                    List<OfferLetter> offerLetters = offerLetterService.getOfferLettersForUser(user);
                    model.addAttribute("offerLetters", offerLetters);
                }
            }
        }

        return "offer-letters";
    }

    private void generatePdfDocument(FormData formData, OutputStream outputStream) throws IOException, java.io.IOException {
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // PDF styling
        PdfFont font = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
        document.setFont(font);
        document.setFontSize(12);

        // Create a header with logo and company name side by side
        try {
            // Use getResourceAsStream instead of getResource().getPath() for better compatibility
            byte[] logoBytes = getClass().getClassLoader().getResourceAsStream("static/img1.png").readAllBytes();
            Image logo = new Image(ImageDataFactory.create(logoBytes));
            // Make the logo smaller
            logo.setWidth(60);

            // Create a single header paragraph that will contain the logo, company name, and tagline
            Paragraph header = new Paragraph();

            // Add the logo to the header
            header.add(logo);
            header.add("\n");

            // Add company name to the header
            header.add(new Text("Acentrik Technology Solutions LLC")
                    .setBold()
                    .setFontSize(16));
            header.add("\n");

            // Add tagline to the header
            header.add(new Text("Passion, Innovation & Trust")
                    .setBold()
                    .setFontSize(12));

            // Set the alignment for the entire header
            header.setTextAlignment(TextAlignment.CENTER);

            // Add the complete header to the document
            document.add(header);

        } catch (Exception e) {
            // Fallback to text header if image loading fails
            System.err.println("Failed to load logo image: " + e.getMessage());
            e.printStackTrace();

            // Header without logo as fallback - using a single paragraph for consistent alignment
            Paragraph header = new Paragraph();

            // Add company name to the header
            header.add(new Text("Acentrik Technology Solutions LLC")
                    .setBold()
                    .setFontSize(16));
            header.add("\n");

            // Add tagline to the header
            header.add(new Text("Passion, Innovation & Trust")
                    .setBold()
                    .setFontSize(12));

            // Set the alignment for the entire header
            header.setTextAlignment(TextAlignment.CENTER);

            // Add the complete header to the document
            document.add(header);
        }

        // Date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy");
        String formattedToday = LocalDate.now().format(formatter);
        document.add(new Paragraph(formattedToday)
                .setTextAlignment(TextAlignment.LEFT));

        // Offer content (same as before)
        String formattedJoiningDate = formData.getJoiningDate().format(formatter);
        document.add(new Paragraph("Offer of Internship")
                .setBold()
                .setFontSize(12)
                .setTextAlignment(TextAlignment.LEFT));

        document.add(new Paragraph("Dear " + formData.getFirstName() + " " + formData.getLastName() + ","));

        document.add(new Paragraph("It's my pleasure to offer you a " + formData.getRole() + "  role at Acentrik. Your internship will be from " + formattedJoiningDate+ ".").setTextAlignment(TextAlignment.JUSTIFIED));

        document.add(new Paragraph("On behalf of Acentrik Technology Solutions, LLC, 4425 W Airport fwy, Suite 117, Irving, TX-75062, we are pleased to welcome you. You will report to " + formData.getManager() + " (Reporting Manager) during your internship period with us.").setTextAlignment(TextAlignment.JUSTIFIED));

        document.add(new Paragraph("Your Job responsibilities include:")
                .setBold());

        document.add(new Paragraph(
                """
                        \t• Conduct business process analysis to understand existing workflow and systems, document current processes and systems.
                        \t• Create requirements for new processes, develop use cases and manage requirement changes.
                        \t• Have strong technical acumen with the ability to translate information and research into non-technical language as necessary to effectively communicate across teams.
                        \t• Gather, summarize, and verify information used to populate reports and deliverables.
                        \t• Facilitate requirement reviews with stakeholders, perform QA testing and conduct UAT with business/process teams.
                        \t• Maintain documentation related to CMDB processes, procedures, and configurations."""
        ).setTextAlignment(TextAlignment.JUSTIFIED));


        document.add(new Paragraph()
                .add(new Text("Company Agreements: ").setBold())
                .add("For the purpose of Federal Immigration Law, you will be required to provide the evidence of your identity and eligibility for internship in the United States. Such documentation must be provided to us within three business days of your date of hire with Acentrik Technology Solutions, LLC, or your internship may be terminated.").setTextAlignment(TextAlignment.JUSTIFIED)
        );

        document.add(new Paragraph()
                .add(new Text("At-Will Employment: ").setBold())
                .add("If you accept this offer, you understand and agree that your employment with the Company is for no specified period and constitutes \"at-will\" employment. As a result, you will be free to resign at any time or for any reason or no reason. The company will similarly have the right to end its employment relationship with you at any time, with or without notice and with or without cause. You understand and agree that any representation to the contrary is unauthorized and not valid unless obtained, written, and signed by the company manager.").setTextAlignment(TextAlignment.JUSTIFIED)
        );
        document.add(new Paragraph("If you have any questions or need further information, please feel free to contact me at 972-799-6164 or kishore.medikonda@acentriktech.com. We look forward to seeing you and we offer you a very warm welcome.").setTextAlignment(TextAlignment.JUSTIFIED));

        document.add(new Paragraph("Sincerely,"));

        document.add(new Paragraph("Kishore Medikonda\nHR Director\nAcentrik Technology Solutions LLC"));

        document.close();
    }
}
