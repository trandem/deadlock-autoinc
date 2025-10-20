# Multi-stage build optimized for speed
# Using Java 21 for Virtual Threads (Project Loom) support

# Build stage - optimized for Maven caching
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy Maven wrapper and pom.xml first (better caching)
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

# Download dependencies (this layer is cached unless pom.xml changes)
RUN ./mvnw dependency:resolve -B

# Copy source code
COPY src ./src

# Build the application (skip tests for faster builds)
RUN ./mvnw clean package -DskipTests -T 1C

# Runtime stage - minimal JRE image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install curl for health check (alpine uses apk)
RUN apk add --no-cache curl

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Change ownership
RUN chown appuser:appgroup app.jar

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run with Virtual Threads optimization
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-jar", "app.jar"]
