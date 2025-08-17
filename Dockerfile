# --- Build Stage ---
# Use the full JDK image to build the application
FROM openjdk:17-jdk-slim as builder

# Set the working directory
WORKDIR /app

# Copy the Maven wrapper and project files
COPY ./smartcontactmanager/.mvn/ ./.mvn/
COPY ./smartcontactmanager/mvnw .
COPY ./smartcontactmanager/pom.xml .

# *** THIS IS THE FIX ***
# Make the Maven wrapper executable
RUN chmod +x ./mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy the application source code
COPY ./smartcontactmanager/src/ ./src/

# Build the application and create the JAR file
RUN ./mvnw package -DskipTests


# --- Run Stage ---
# Use the smaller, more secure slim JRE image to run the application
FROM openjdk:17-slim

# Set the working directory
WORKDIR /app

# Copy the built JAR file from the 'builder' stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the port the application runs on
EXPOSE 8080

# The command to run the application when the container starts
ENTRYPOINT ["java","-jar","app.jar"]