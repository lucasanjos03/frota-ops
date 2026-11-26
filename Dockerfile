# =============================================================================
# FleetOps — imagem de produção
# Build multi-stage: compila com Maven/JDK 21 e executa apenas com a JRE 21.
# =============================================================================

# ---- Estágio 1: build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Camada de dependências: só é refeita quando o pom.xml muda (cache de build)
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Código-fonte e empacotamento.
# -DskipTests: a suíte é de integração (@SpringBootTest) e exige um banco vivo,
# que não existe durante o build da imagem.
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Estágio 2: runtime ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Executa sem privilégios de root
RUN useradd -m appuser

COPY --from=build /app/target/*.jar app.jar
USER appuser

# A porta efetiva é definida pelo provedor (variável PORT); 8081 é o padrão local
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
