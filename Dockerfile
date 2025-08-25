# ===== Stage 1: Build jar =====
FROM maven:3.9.3-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom + source code
COPY pom.xml .
COPY src ./src

# Build jar (bỏ qua test)
RUN mvn clean package -DskipTests

# ===== Stage 2: Run jar =====
FROM openjdk:17-jdk-slim
WORKDIR /app

# Copy jar từ stage build
COPY --from=build /app/target/*.jar app.jar

# Expose port Render cấp
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
