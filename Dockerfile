# Dockerfile para HotelImpact
# Multi-stage build: build com JDK, runtime com JRE
# Java 21, Spring Boot 4.2.0-M2, Maven Wrapper

# ============================================================
# ESTÁGIO 1: Build (JDK 21)
# ============================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

# Instalar dependências necessárias para o build
RUN apk add --no-cache bash

WORKDIR /app

# Copiar wrapper Maven e arquivos de configuração primeiro (cache de dependências)
COPY mvnw mvnw.cmd ./
COPY .mvn/ .mvn/
COPY pom.xml .

# Baixar dependências (cache de layer)
RUN ./mvnw dependency:go-offline -B

# Copiar código fonte
COPY src/ src/

# Build do JAR (pula testes para acelerar; testes rodam no CI)
RUN ./mvnw package -DskipTests -B

# ============================================================
# ESTÁGIO 2: Runtime (JRE 21)
# ============================================================
FROM eclipse-temurin:21-jre-alpine AS runner

# Usuário não privilegiado
RUN addgroup -g 1001 -S hotelimpact && \
    adduser -S -G hotelimpact -u 1001 hotelimpact

WORKDIR /app

# Copiar JAR do estágio de build
COPY --from=builder /app/target/*.jar app.jar

# Diretórios para logs e dados temporários
RUN mkdir -p /app/logs /app/tmp && chown -R hotelimpact:hotelimpact /app

# Usuário não privilegiado
USER hotelimpact

# Variáveis de ambiente padrão
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    SPRING_PROFILES_ACTIVE=prod

# Porta exposta (Render define PORT, padrão 8080 para execução local em container)
EXPOSE 8080

# Health check básico
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Entry point
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
