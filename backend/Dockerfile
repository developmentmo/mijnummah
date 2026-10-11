# Production image of the backend. Build from this directory:  docker build -t mijnummah-backend .
# Tests are not run here: CI runs `./mvnw verify` (with PostgreSQL) before an image is built and released.

FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -ntp -q dependency:go-offline
COPY src src
RUN ./mvnw -B -ntp -q -DskipTests package \
    && java -Djarmode=tools -jar target/mijnummah-backend-*.jar extract --layers --launcher --destination /extracted

FROM eclipse-temurin:21-jre
# Unprivileged user; the application needs no write access to its own files.
RUN groupadd --system --gid 10001 mijnummah \
    && useradd --system --uid 10001 --gid mijnummah --no-create-home --shell /usr/sbin/nologin mijnummah
WORKDIR /app
COPY --from=build /extracted/dependencies/ ./
COPY --from=build /extracted/spring-boot-loader/ ./
COPY --from=build /extracted/snapshot-dependencies/ ./
COPY --from=build /extracted/application/ ./
USER 10001:10001
ENV TZ=UTC \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
