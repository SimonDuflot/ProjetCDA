# Stage 1: Build the application using Maven
# Use a specific Maven image with the correct Java version
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copy pom.xml first to leverage Docker cache for dependencies
COPY pom.xml .
# Download all dependencies
RUN mvn dependency:go-offline -B

# Copy the rest of the source code
COPY src ./src

# Package the application, skipping tests (tests will run in CI/locally)
RUN mvn package -DskipTests -B

# Stage 2: Create the final lightweight runtime image
# Use a JRE image matching the Java version, preferably slim or alpine
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the packaged JAR file from the build stage
# Use wildcard to copy the jar without knowing the exact version in the name
COPY --from=builder /app/target/*.jar app.jar

# Expose the port the application runs on
EXPOSE 8080

# Command to run the application
# Use exec form for better signal handling
ENTRYPOINT ["java", "-Xmx512m", "-jar", "/app/app.jar"]

# Optional: Add healthcheck instruction if needed later
# HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 CMD curl -f http://localhost:8080/actuator/health || exit 1
