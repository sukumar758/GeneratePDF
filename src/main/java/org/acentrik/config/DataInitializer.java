package org.acentrik.config;

import org.acentrik.model.User;
import org.acentrik.repository.UserRepository;
import org.acentrik.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;

    @Value("${app.admin.auto-recreate:true}")
    private boolean adminAutoRecreate;

    @Override
    public void run(String... args) throws Exception {
        // Check if admin user exists and recreate only if auto-recreate is enabled
        if (userRepository.findByUsername("Admin").isEmpty() && adminAutoRecreate) {
            // Use UserService to create admin user
            User adminUser = userService.createAdminUser();
            System.out.println("Admin user created");
        }

        // Check if regular user exists
        if (userRepository.findByUsername("User").isEmpty()) {
            User regularUser = new User("User", passwordEncoder.encode("User123!"), "USER");
            userRepository.save(regularUser);
            System.out.println("Regular user created");
        }
    }
}
