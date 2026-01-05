# Build stage
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy Gradle wrapper and metadata first for better layer caching
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Fix execute bit for gradlew in Linux container
RUN chmod +x gradlew

# Copy source
COPY src src

# Build fat jar (skip tests for faster deploy)
RUN ./gradlew clean build -x test


# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# Render provides PORT; app already binds to ${PORT:8080}
ENV PORT=8080

COPY --from=build /app/build/libs/*.jar /app/app.jar

EXPOSE 8080

CMD ["java","-jar","/app/app.jar"]
