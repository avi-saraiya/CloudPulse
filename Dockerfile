# Builds the jar inside the image, then runs it. Database settings come from environment variables.
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY src src
RUN ./mvnw -q package -DskipTests
EXPOSE 8080
CMD ["java", "-jar", "target/cloudpulse-0.0.1-SNAPSHOT.jar"]
