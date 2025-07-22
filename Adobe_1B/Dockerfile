# Use a lightweight Java 17 image with Linux/amd64
FROM --platform=linux/amd64 eclipse-temurin:17-jdk-jammy

# Set the working directory inside container
WORKDIR /app

# Copy the pre-built shaded JAR into the container
COPY target/Adobe_1B-1.0-SNAPSHOT-shaded.jar app.jar

# Set command to run the JAR when container starts
ENTRYPOINT ["java", "-jar", "app.jar"]
