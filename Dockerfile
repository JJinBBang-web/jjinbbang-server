FROM eclipse-temurin:21-jdk-alpine AS builder

COPY security/blocked.certs /tmp/blocked.certs
RUN grep -qx 'Algorithm=SHA-256' "$JAVA_HOME/lib/security/blocked.certs" \
    && sort -u -o /tmp/merged-blocked.certs "$JAVA_HOME/lib/security/blocked.certs" /tmp/blocked.certs \
    && mv /tmp/merged-blocked.certs "$JAVA_HOME/lib/security/blocked.certs" \
    && rm /tmp/blocked.certs

WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine

COPY security/blocked.certs /tmp/blocked.certs
RUN grep -qx 'Algorithm=SHA-256' "$JAVA_HOME/lib/security/blocked.certs" \
    && sort -u -o /tmp/merged-blocked.certs "$JAVA_HOME/lib/security/blocked.certs" /tmp/blocked.certs \
    && mv /tmp/merged-blocked.certs "$JAVA_HOME/lib/security/blocked.certs" \
    && rm /tmp/blocked.certs

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=builder --chown=spring:spring /app/build/libs/*.jar app.jar

USER spring:spring
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
