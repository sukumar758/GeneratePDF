# Build stage
FROM maven:3.8.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy only pom.xml first to leverage Docker cache
COPY pom.xml .

# Download dependencies and cache them in a separate layer
# Use -T 1C to use 1 thread per available core
RUN mvn dependency:go-offline -T 1C

# Copy source code
COPY src/ /app/src/

# Build the application with parallel compilation
RUN mvn package -DskipTests -T 1C

# Run stage - using a smaller base image
FROM eclipse-temurin:17-jre
WORKDIR /app

# Create a non-root user and install wget for health check in a single layer
RUN apt-get update && apt-get install -y wget && \
    groupadd -g 1001 appuser && \
    useradd -u 1001 -g appuser -s /bin/bash -m appuser && \
    mkdir -p /app && \
    chown -R appuser:appuser /app && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copy the JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Switch to non-root user
USER appuser

# Expose the application port
EXPOSE 8091

# Health check
HEALTHCHECK --interval=30s --timeout=3s --retries=3 CMD wget -q --spider http://localhost:8091/actuator/health || exit 1

# Run the application with optimized JVM settings
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
