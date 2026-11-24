# Imagem da API Spring Boot. O contexto do build é a RAIZ do repositório, porque o
# Script_Pokedex.sql (fonte dos dados) fica lá: docker compose build backend
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY Script_Pokedex.sql Script_Pokedex_Geracoes.sql ./
COPY backend/pom.xml backend/
RUN mvn -f backend/pom.xml -B -q dependency:go-offline
COPY backend/src backend/src
RUN mvn -f backend/pom.xml -B -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/backend/target/pokedex-api-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
