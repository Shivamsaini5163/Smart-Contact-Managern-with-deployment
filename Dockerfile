# --- Build Stage ---
# This stage builds your Java application
FROM openjdk:17-jdk-slim as builder

# Set the working directory for the build
WORKDIR /app

# Copy only the necessary build files first
COPY ./smartcontactmanager/.mvn/ ./.mvn/
COPY ./smartcontactmanager/mvnw .
COPY ./smartcontactmanager/pom.xml .

# Make the maven wrapper executable
RUN chmod +x ./mvnw

# Download all project dependencies
RUN ./mvnw dependency:go-offline

# Copy the rest of your application's source code
COPY ./smartcontactmanager/src/ ./src/

# Build the application into a JAR file
RUN ./mvnw package -DskipTests


# --- Run Stage ---
# This stage runs your built application in a smaller, secure image
FROM openjdk:17-slim

# Set the working directory
WORKDIR /app

# Copy the built JAR file from the 'builder' stage
COPY --from=builder /app/target/*.jar app.jar

# Tell Render that your application will listen on port 8080
EXPOSE 8080

# The command to start your application
ENTRYPOINT ["java","-jar","app.jar"]
