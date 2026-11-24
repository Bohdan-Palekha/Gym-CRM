# Gym CRM — one Dockerfile for all three Spring Boot services.
#
# discovery-server, gym-crm and trainer-workload-service are independent Maven projects with
# identical build mechanics, so the service is selected with build args instead of keeping three
# copies of the same file (build context = repository root):
#
#   docker build -t gym-crm --build-arg MODULE=gym-crm --build-arg PORT=8080 .
#
#   MODULE  discovery-server | gym-crm | trainer-workload-service   (required)
#   PORT    HTTP port of the service (EXPOSE + HEALTHCHECK)          (default 8080)
#
# Stages: deps (re-run only when the pom changes) -> package -> layers -> runtime.
# JDK 21 LTS builds and runs everything; bytecode level stays at the poms' 17.

ARG MAVEN_IMAGE=maven:3.9.9-eclipse-temurin-21
ARG RUNTIME_IMAGE=eclipse-temurin:21-jre

# ---- 1. dependencies: cached until the module's pom.xml changes -------------------------------
# Tag is pinned in the ARG default above; hadolint cannot resolve ARGs.
# hadolint ignore=DL3006
FROM ${MAVEN_IMAGE} AS deps
ARG MODULE
WORKDIR /build
RUN test -n "${MODULE}" || { echo "build arg MODULE is required (discovery-server | gym-crm | trainer-workload-service)" >&2; exit 1; }
COPY ${MODULE}/pom.xml ./
RUN mvn -B -ntp dependency:go-offline

# ---- 2. compile + package (tests run in CI, not during the image build) ------------------------
FROM deps AS package
ARG MODULE
COPY ${MODULE}/src ./src
RUN mvn -B -ntp -DskipTests package \
 && cp target/*.jar app.jar

# ---- 3. split the fat jar into Spring Boot layers (dependencies rarely change -> small pushes) --
# Tag is pinned in the ARG default above; hadolint cannot resolve ARGs.
# hadolint ignore=DL3006
FROM ${RUNTIME_IMAGE} AS layers
WORKDIR /layers
COPY --from=package /build/app.jar ./
RUN java -Djarmode=layertools -jar app.jar extract

# ---- 4. runtime --------------------------------------------------------------------------------
# Tag is pinned in the ARG default above; hadolint cannot resolve ARGs.
# hadolint ignore=DL3006
FROM ${RUNTIME_IMAGE} AS runtime
ARG MODULE
ARG PORT=8080
LABEL org.opencontainers.image.title="${MODULE}" \
      org.opencontainers.image.description="Gym CRM microservices - ${MODULE}"

# curl backs the HEALTHCHECK. Run as an unprivileged user; /app/logs is writable because logback
# writes rolling files to a relative ./logs in addition to the console.
# hadolint ignore=DL3008
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd --system --gid 10001 app \
 && useradd --system --uid 10001 --gid app --no-create-home --shell /usr/sbin/nologin app \
 && mkdir -p /app/logs \
 && chown -R app:app /app
WORKDIR /app

# least -> most frequently changing, so a code-only change only rewrites the last layer
COPY --from=layers --chown=app:app /layers/dependencies/ ./
COPY --from=layers --chown=app:app /layers/spring-boot-loader/ ./
COPY --from=layers --chown=app:app /layers/snapshot-dependencies/ ./
COPY --from=layers --chown=app:app /layers/application/ ./

USER app
# SERVER_PORT is picked up by every service (explicit ${SERVER_PORT:..} placeholder, or Spring's
# relaxed binding for gym-crm, which sets no server.port). JAVA_TOOL_OPTIONS needs no shell, so
# the exec-form ENTRYPOINT keeps java as PID 1 and SIGTERM reaches Spring's shutdown hooks.
ENV SERVER_PORT=${PORT} \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
EXPOSE ${PORT}

# /actuator/health is public on all three services and exposed in their management settings
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=5 \
  CMD curl -fsS "http://localhost:${SERVER_PORT}/actuator/health" > /dev/null || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
