# syntax=docker/dockerfile:1
# Imagen del backend MUISCA para Render (plan gratuito: 512 MB de RAM, 0,1 CPU).

# ---------- Compilación ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Descarga las dependencias en una capa propia para reutilizarla entre despliegues.
RUN mvn -B -q dependency:go-offline || true
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---------- Ejecución ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 --home /app muisca
COPY --from=build /app/target/muiscaweb-*.jar /app/app.jar
USER muisca

# Ajustes medidos para caber en 512 MB (~330 MB de RSS ejecutando las cadenas ATL + Acceleo):
# heap ≈ 60 % de la RAM, GC serial y JIT de nivel 1 para arrancar rápido con poca CPU.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -Xss512k -XX:MaxMetaspaceSize=180m \
 -XX:ReservedCodeCacheSize=48m -XX:MaxDirectMemorySize=32m -XX:+UseSerialGC -XX:TieredStopAtLevel=1"

# Render inyecta PORT; application.properties lo usa (server.port=${PORT:8080}).
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
