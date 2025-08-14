Welcome to Acentrik

## Docker Build Optimizations

The Docker build process has been optimized to improve build speed and reduce image size:

### 1. Added `.dockerignore`
- Excludes unnecessary files from the Docker build context
- Reduces the amount of data sent to the Docker daemon
- Speeds up the build process by reducing context size

### 2. Optimized Dockerfile

#### Build Stage Improvements:
- Added parallel compilation with `-T 1C` flag for Maven
- Maintained clear separation of dependency resolution and compilation for better caching
- Added comments to clarify the purpose of each step

#### Runtime Stage Improvements:
- Switched from `openjdk:17-jdk-slim-bullseye` to `eclipse-temurin:17-jre-alpine`
  - The Alpine image is significantly smaller (approximately 70% smaller)
  - JRE-only image contains just what's needed to run the application
- Used Alpine's package manager (`apk`) instead of `apt-get` for faster installation
- Combined user creation and permission setting into a single RUN command to reduce layers
- Added JVM optimization flags to better utilize container resources

### 3. Updated docker-compose.yml
- Added timezone setting for Alpine-based image
- Ensures application has correct time information

## Quick Start with Docker

### Prerequisites
- Docker and Docker Compose installed on your system
- MySQL Database (optional, as the application now uses H2 in-memory database by default)

### Building and Running with Docker

1. Copy `.env.example` to `.env` in the project root and update the following environment variables:
   ```
   # Database Configuration
   MYSQL_ROOT_PASSWORD=your_mysql_root_password
   SPRING_DATASOURCE_USERNAME=root
   SPRING_DATASOURCE_PASSWORD=your_database_password
   SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/pdf?useSSL=false&serverTimezone=UTC

   # Email Configuration
   SPRING_MAIL_HOST=smtp.gmail.com
   SPRING_MAIL_PORT=587
   SPRING_MAIL_USERNAME=your_email@gmail.com
   SPRING_MAIL_PASSWORD=your_email_app_password
   SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
   SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true

   # Application Configuration
   SERVER_PORT=8091
   SPRING_JPA_HIBERNATE_DDL_AUTO=update
   SPRING_JPA_SHOW_SQL=true
   ```
   Replace the placeholder values with your actual credentials. For Gmail, you'll need to use an App Password instead of your regular password. Note that the database URL uses "mysql" as the hostname, which is the service name defined in docker-compose.yml.

2. Build the Docker images:
```bash
docker-compose build
```

3. Start the containers:
```bash
docker-compose up -d
```

4. Check the logs:
```bash
docker-compose logs -f
```

5. Access the application at: http://localhost:8091

### Stopping the Application

To stop the containers:
```bash
docker-compose down
```

For more detailed deployment instructions and security recommendations, please refer to the [Security Report and Deployment Guide](security-report-and-deployment-guide.md).

### Running with the Docker Script

We've provided a convenient script to run the application in Docker with various configuration options:

1. Make the script executable:
   ```bash
   chmod +x run-docker.sh
   ```

2. Run with default configuration (uses H2 in-memory database):
   ```bash
   ./run-docker.sh
   ```

3. Run with MySQL on a specific host (override the default H2 database):
   ```bash
   ./run-docker.sh --mysql-host your-mysql-host --mysql-port 3306
   ```

4. Run with a custom application port:
   ```bash
   ./run-docker.sh --app-port 8080
   ```

5. Combine options as needed:
   ```bash
   ./run-docker.sh --use-h2 --app-port 8080
   ```

This script simplifies running the Docker container with the right environment variables and configuration.

## Running Without Docker

### Prerequisites
- Java 17 or higher
- Maven
- MySQL Database (optional, as the application now uses H2 in-memory database by default)

### Database Setup (Optional - Only if using MySQL)
1. Install MySQL if you haven't already
2. Create a database named `pdf`:
   ```sql
   CREATE DATABASE pdf;
   ```
3. Make sure the MySQL server is running on port 3306
4. Configure the application to use MySQL as described in the Configuration section below

### Running the Application
1. Clone the repository
2. Navigate to the project directory
3. Build the project:
   ```bash
   ./mvnw clean package
   ```
4. Run the application:
   ```bash
   java -jar target/generatePdf-0.0.1-SNAPSHOT.jar
   ```

   Alternatively, you can run it with Maven:
   ```bash
   ./mvnw spring-boot:run
   ```

5. Access the application at: http://localhost:8091

### Configuration

#### Database Configuration
The application now uses H2 in-memory database by default:
- Database URL: jdbc:h2:mem:pdf
- Database Username: sa
- Database Password: (empty)

If you want to use MySQL instead, you can:
1. Modify the `src/main/resources/application.properties` file
2. Or set environment variables:
   ```bash
   export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/pdf?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
   export SPRING_DATASOURCE_USERNAME=your_username
   export SPRING_DATASOURCE_PASSWORD=your_password
   export SPRING_DATASOURCE_DRIVER=com.mysql.cj.jdbc.Driver
   export SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.MySQLDialect
   ```

#### Email Configuration
The application uses Gmail SMTP for sending emails. To configure email functionality:

1. **Gmail Account Setup**:
   - You need a Gmail account to use as the sender for emails
   - For security reasons, Google requires using an App Password instead of your regular password if you have 2-Step Verification enabled

2. **Generating a Gmail App Password**:
   - Go to your Google Account settings: https://myaccount.google.com/
   - Select "Security" from the left menu
   - Under "Signing in to Google", select "App Passwords" (you may need to enable 2-Step Verification first)
   - Select "Mail" as the app and "Other (Custom name)" as the device
   - Enter "Acentrik App" as the name
   - Click "Generate" and copy the 16-character password that appears

3. **Configuring the Application**:
   - Set the following environment variables:
     ```bash
     export SPRING_MAIL_HOST=smtp.gmail.com
     export SPRING_MAIL_PORT=587
     export SPRING_MAIL_USERNAME=your_email@gmail.com
     export SPRING_MAIL_PASSWORD=your_16_character_app_password
     ```
   - Or modify the `src/main/resources/application.properties` file directly

4. **Troubleshooting**:
   - If emails are not being sent, check that your Gmail App Password is correct
   - Ensure that your Gmail account doesn't have any security restrictions that might block the application
   - If you recently changed your Gmail password, you'll need to generate a new App Password
