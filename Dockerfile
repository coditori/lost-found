# Build stage
FROM maven:3.9.5-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build the application
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -g 1001 -S spring && \
    adduser -S spring -u 1001

# Create directories for uploads and logs
RUN mkdir -p /app/uploads /app/logs && \
    chown -R spring:spring /app

# Copy the jar file from build stage
COPY --from=build /app/target/*.jar app.jar

# Change to non-root user
USER spring:spring

# Expose port
EXPOSE 8080

# Set JVM options
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"] 