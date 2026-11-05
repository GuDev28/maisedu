# syntax=docker/dockerfile:1
# Back-end do MaisEdu: compila com o Maven Wrapper e roda só com a JRE.

FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src src
# O cache do ~/.m2 acelera os builds seguintes (requer BuildKit, padrão no Docker atual)
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 10001 maisedu
COPY --from=build /app/target/*.jar /app/app.jar
USER maisedu
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
