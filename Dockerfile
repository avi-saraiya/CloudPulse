# Multi-stage build: stage 1 compiles the app with the full JDK, stage 2 only contains the JRE + the jar.
# The final image is therefore much smaller and contains no source code or build tools.

# ---------- Stage 1: build ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Copy only the Maven files first and download dependencies. Docker caches this layer,
# so dependencies are re-downloaded only when pom.xml changes, not on every code change.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -q dependency:go-offline

# Now copy the source and build the jar. Tests run in the Jenkins pipeline, so we skip them here.
COPY src src
RUN ./mvnw -q package -DskipTests

# ---------- Stage 2: run ----------
FROM eclipse-temurin:21-jre-alpine

# Create and use a normal (non-root) user: if the app were ever compromised, it would not be root.
RUN addgroup -S cloudpulse && adduser -S cloudpulse -G cloudpulse
USER cloudpulse
WORKDIR /app

COPY --from=build /workspace/target/cloudpulse-*.jar app.jar

# Documentation only: the app listens on 8080. DB_URL, DB_USERNAME and DB_PASSWORD are
# passed in when the container starts (docker compose / Kubernetes), never baked into the image.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
