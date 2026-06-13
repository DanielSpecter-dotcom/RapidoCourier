# Etapa 1: Compilación y empaquetado autónomo
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app

# Copiamos el pom.xml para descargar las dependencias y aprovechar la caché de capas de Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiamos el código fuente estructurado y compilamos omitiendo pruebas unitarias
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Imagen de ejecución ligera y segura
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Comando de ejecución del microservicio
ENTRYPOINT ["java", "-jar", "app.jar"]