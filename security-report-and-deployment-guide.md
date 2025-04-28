# Security Audit Report and Deployment Guide

## Security Vulnerabilities and Recommendations

### 1. Authentication and Authorization

#### Vulnerabilities:
- Hard-coded credentials in `UserService.java` (Admin123, User123)
- Weak default passwords
- No password policy enforcement
- No account lockout mechanism
- Default credentials displayed on login page
- No multi-factor authentication

#### Recommendations:
- Remove hard-coded credentials and use environment variables or a secure vault
- Implement a strong password policy (min length, complexity requirements)
- Add account lockout after multiple failed login attempts
- Remove default credentials from login page
- Consider implementing multi-factor authentication for admin users
- Add password expiration and history policies

### 2. Sensitive Data Exposure

#### Vulnerabilities:
- Plaintext credentials in application.properties
- Database using root user
- Email credentials in application.properties
- SQL queries being logged (show-sql=true)
- No HTTPS configuration

#### Recommendations:
- Use environment variables or a secure vault for sensitive credentials
- Create a dedicated database user with minimal privileges
- Enable SSL for database connections
- Disable SQL query logging in production
- Configure HTTPS and redirect HTTP to HTTPS
- Add secure and httpOnly flags to cookies

### 3. Input Validation and Output Encoding

#### Vulnerabilities:
- No input validation in FormData model
- No sanitization of user input
- Potential XSS vulnerabilities in templates
- Filename in Content-Disposition constructed from user input

#### Recommendations:
- Add validation annotations to FormData model (e.g., @NotBlank, @Email, @Size)
- Implement server-side validation in controllers
- Use Thymeleaf's th:text instead of direct variable interpolation
- Sanitize user input before using it in filenames or other contexts
- Add maxlength attributes to input fields

### 4. CSRF Protection

#### Vulnerabilities:
- No explicit CSRF configuration
- No CSRF tokens in forms

#### Recommendations:
- Explicitly configure CSRF protection in SecurityConfig
- Ensure all forms include CSRF tokens
- Add SameSite cookie attribute

### 5. Error Handling

#### Vulnerabilities:
- Exception details exposed to users
- No centralized error handling

#### Recommendations:
- Implement a global exception handler
- Return generic error messages to users
- Log detailed error information for debugging
- Add proper error pages for different HTTP status codes

### 6. Security Headers

#### Vulnerabilities:
- No security headers configured

#### Recommendations:
- Add Content-Security-Policy header
- Add X-Content-Type-Options: nosniff
- Add X-Frame-Options: DENY
- Add X-XSS-Protection: 1; mode=block
- Add Strict-Transport-Security header

### 7. Docker Security

#### Vulnerabilities:
- Container runs as root
- No health check
- No explicit port exposure
- No security hardening

#### Recommendations:
- Add a non-root user to run the application
- Add a health check endpoint and Docker HEALTHCHECK
- Explicitly expose only necessary ports
- Use multi-stage builds to reduce attack surface
- Scan container images for vulnerabilities
- Use read-only file systems where possible

### 8. Dependency Management

#### Vulnerabilities:
- Outdated dependencies
- No dependency vulnerability scanning

#### Recommendations:
- Update all dependencies to latest versions
- Add OWASP Dependency Check plugin
- Regularly scan for vulnerable dependencies
- Remove unused dependencies

### 9. Logging and Monitoring

#### Vulnerabilities:
- Insufficient logging for security events
- No audit trail for sensitive operations

#### Recommendations:
- Implement comprehensive logging for authentication events
- Log all sensitive operations (PDF generation, email sending)
- Consider using a centralized logging system
- Implement monitoring and alerting for suspicious activities

## Deployment Guide

### Prerequisites
- Java 17 or later
- Docker and Docker Compose
- MySQL Database
- SMTP Server for email functionality

### Step 1: Configure Environment Variables

Create a `.env` file with the following variables:
```
# Application
SPRING_APPLICATION_NAME=GeneratePDF
SERVER_PORT=8091

# Database
SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/pdf?useSSL=true&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=pdfapp
SPRING_DATASOURCE_PASSWORD=<strong-password>

# JPA
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_JPA_SHOW_SQL=false

# Mail
SPRING_MAIL_HOST=<your-smtp-host>
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=<your-email>
SPRING_MAIL_PASSWORD=<your-email-password>
SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true

# Security
ADMIN_USERNAME=admin
ADMIN_PASSWORD=<strong-admin-password>
USER_USERNAME=user
USER_PASSWORD=<strong-user-password>
```

### Step 2: Create a Secure Dockerfile

Create a new Dockerfile in the project root:
```Dockerfile
# Build stage
FROM maven:3.8.6-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src/ /app/src/
RUN mvn package -DskipTests

# Run stage
FROM openjdk:17-jdk-slim-bullseye
WORKDIR /app

# Create a non-root user to run the application
RUN addgroup --system --gid 1001 appuser && \
    adduser --system --uid 1001 --gid 1001 appuser

# Copy the JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Set permissions
RUN chown -R appuser:appuser /app
USER appuser

# Expose the application port
EXPOSE 8091

# Health check
HEALTHCHECK --interval=30s --timeout=3s --retries=3 CMD wget -q --spider http://localhost:8091/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### Step 3: Create a Docker Compose File

Create a `docker-compose.yml` file:
```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: pdf-mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: pdf
      MYSQL_USER: ${SPRING_DATASOURCE_USERNAME}
      MYSQL_PASSWORD: ${SPRING_DATASOURCE_PASSWORD}
    volumes:
      - mysql-data:/var/lib/mysql
    networks:
      - pdf-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  pdf-app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: pdf-app
    depends_on:
      mysql:
        condition: service_healthy
    ports:
      - "8091:8091"
    env_file:
      - .env
    networks:
      - pdf-network
    restart: unless-stopped

networks:
  pdf-network:
    driver: bridge

volumes:
  mysql-data:
```

### Step 4: Build and Run the Application

1. Build the Docker images:
```bash
docker-compose build
```

2. Start the containers:
```bash
docker-compose up -d
```

3. Check the logs:
```bash
docker-compose logs -f
```

### Step 5: Access the Application

- The application will be available at: http://localhost:8091
- Login with the credentials specified in your environment variables

### Step 6: Secure Your Production Environment

1. Set up a reverse proxy (like Nginx) with SSL termination
2. Configure firewall rules to restrict access
3. Set up monitoring and alerting
4. Implement regular backups
5. Schedule regular security updates

### Step 7: Maintenance

1. Regularly update dependencies:
```bash
mvn versions:display-dependency-updates
```

2. Scan for vulnerabilities:
```bash
mvn org.owasp:dependency-check-maven:check
```

3. Monitor application logs for suspicious activities
4. Perform regular security audits

## Conclusion

This deployment guide provides a secure way to deploy the application. By following these steps and implementing the security recommendations, you can significantly improve the security posture of the application.

Remember to:
- Never store sensitive credentials in code or version control
- Regularly update all dependencies
- Monitor for suspicious activities
- Perform regular security audits
- Keep all systems patched and updated