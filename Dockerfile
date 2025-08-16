# --- Build Stage ---
# Use an official OpenJDK image as the base for building the application
FROM openjdk:17-jdk-slim as builder

# Set the working directory inside the container
WORKDIR /app

# Copy the Maven wrapper and project definition file
COPY ./smartcontactmanager/.mvn/ ./.mvn/
COPY ./smartcontactmanager/mvnw .
COPY ./smartcontactmanager/pom.xml .

# Download the project dependencies
RUN ./mvnw dependency:go-offline

# Copy the rest of the application's source code
COPY ./smartcontactmanager/src/ ./src/

# Build the application, creating the JAR file
RUN ./mvnw package -DskipTests


# --- Run Stage ---
# Use a smaller, more secure JRE image to run the application
FROM openjdk:17-jre-slim

# Set the working directory
WORKDIR /app

# Copy the built JAR file from the 'builder' stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the port the application runs on
EXPOSE 8080

# The command to run the application
ENTRYPOINT ["java","-jar","app.jar"]