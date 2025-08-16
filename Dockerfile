# Use a standard base image with Java 17 installed
FROM openjdk:17-jdk-slim

# Set an argument to find the JAR file created by the build process
ARG JAR_FILE=target/*.jar

# Copy the JAR file from your project's build folder into the container
COPY ${JAR_FILE} app.jar

# Tell the container to run the Spring Boot application when it starts
ENTRYPOINT ["java","-jar","/app.jar"]