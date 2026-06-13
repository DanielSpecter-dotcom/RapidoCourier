# Etapa 1: Compilación y empaquetado
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app

# Copiamos el pom y descargamos las dependencias para aprovechar la caché de Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiamos el código fuente y compilamos omitiendo los tests
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Imagen de ejecución ligera (Usando Temurin que es el estándar actual)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Comando de inicio
ENTRYPOINT ["java", "-jar", "app.jar"]