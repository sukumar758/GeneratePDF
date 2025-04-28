package org.acentrik.config;

import org.acentrik.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import jakarta.annotation.PostConstruct;
import java.util.Collections;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final CustomAuthenticationEventPublisher authenticationEventPublisher;

    @Autowired
    public SecurityConfig(
            UserService userService, 
            PasswordEncoder passwordEncoder,
            CustomAuthenticationEventPublisher authenticationEventPublisher) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationEventPublisher = authenticationEventPublisher;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        ProviderManager authenticationManager = new ProviderManager(Collections.singletonList(authenticationProvider()));
        authenticationManager.setAuthenticationEventPublisher(authenticationEventPublisher);
        return authenticationManager;
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userService)
            .passwordEncoder(passwordEncoder);
        auth.authenticationEventPublisher(authenticationEventPublisher);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/css/**", "/js/**", "/images/**", "/*.png", "/*.jpg", "/*.jpeg", "/*.gif").permitAll()
                .requestMatchers("/").permitAll()
                .requestMatchers("/login").permitAll()
                .requestMatchers("/register").permitAll()
                .requestMatchers("/forgot-password").permitAll()
                .requestMatchers("/reset-password").permitAll()
                .requestMatchers("/form").hasRole("ADMIN")
                .requestMatchers("/generatePdf").hasAnyRole("ADMIN", "USER")
                .requestMatchers("/eform").hasRole("ADMIN")
                .requestMatchers("/emailPdf").hasRole("ADMIN")
                .requestMatchers("/dashboard").hasRole("ADMIN")
                .requestMatchers("/profile").hasAnyRole("ADMIN", "USER")
                .requestMatchers("/viewOfferLetter").hasAnyRole("ADMIN", "USER")
                .requestMatchers("/downloadOfferLetter").hasAnyRole("ADMIN", "USER")
                .requestMatchers("/updateProfile").hasAnyRole("ADMIN", "USER")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .permitAll()
            )
            // Explicitly enable CSRF protection
            .csrf(csrf -> csrf.ignoringRequestMatchers("/actuator/**"))
            // Add security headers
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; script-src 'self'; img-src 'self' https://randomuser.me; style-src 'self' https://fonts.googleapis.com https://cdnjs.cloudflare.com; font-src 'self' https://fonts.gstatic.com https://cdnjs.cloudflare.com; connect-src 'self'; object-src 'self'; media-src 'self'"))
                .frameOptions(frame -> frame.deny())
            )
            // Session management
            .sessionManagement(session -> session
                .sessionFixation().migrateSession()
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            );

        return http.build();
    }

    @PostConstruct
    public void initUsers() {
        userService.initializeUsers();
    }
}
