# Etapa 1: Compilacion con Maven y Java 21 (compatible con Render.com y Docker CI)
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# Etapa 2: Runtime ligero con usuario no-root
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app

COPY --from=build /build/target/Ejercicio5Cliente-0.0.1-SNAPSHOT.jar app.jar
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8082

HEALTHCHECK --interval=15s --timeout=5s --start-period=35s --retries=5 \
  CMD sh -c 'wget -q -O /dev/null http://localhost:${PORT:-8082}/actuator/health || exit 1'

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
