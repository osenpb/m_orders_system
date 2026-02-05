# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Aprovechar el caché de capas para dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

# Install curl for healthcheck
RUN apk add --no-cache curl

# Crear un usuario de sistema para no ejecutar como root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /app

# Copiar el jar con un nombre específico para evitar conflictos
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8761

# Ajustar límites de memoria para tu laptop (256MB es ideal para Eureka)
ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-jar", "app.jar"]