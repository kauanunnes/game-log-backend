# syntax=docker/dockerfile:1

# Compila e separa o jar nas camadas do Spring Boot: dependências mudam pouco e ficam em cache.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q -DskipTests package \
    && java -Djarmode=tools -jar target/game-log-*.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 10001 gamelog
USER gamelog
COPY --from=build /build/extracted/dependencies/ ./
COPY --from=build /build/extracted/spring-boot-loader/ ./
COPY --from=build /build/extracted/snapshot-dependencies/ ./
COPY --from=build /build/extracted/application/ ./
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
