FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Build dependencies (cached layer)
RUN ./mvnw dependency:resolve

# Copy source code
COPY src/ src/

# Package the application
RUN ./mvnw package -DskipTests

# Final image
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy built jar file
COPY --from=0 /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"] 