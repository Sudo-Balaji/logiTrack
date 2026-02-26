# Multi-stage build for production-grade container
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Install curl for downloading agents
RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

# Download OpenTelemetry Java Agent (latest version)
RUN curl -L -o /app/opentelemetry-javaagent.jar \
    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar

# Download Pyroscope Java Agent (latest version)
RUN curl -L -o /app/pyroscope.jar \
    https://github.com/grafana/pyroscope-java/releases/latest/download/pyroscope.jar

# Copy the built jar from build stage
COPY --from=build /build/target/*.jar /app/logitrack.jar

# Expose application port
EXPOSE 8084

# Environment variables for OpenTelemetry
ENV OTEL_SERVICE_NAME=logiTrack
ENV OTEL_TRACES_EXPORTER=otlp
ENV OTEL_METRICS_EXPORTER=otlp
ENV OTEL_LOGS_EXPORTER=otlp
ENV OTEL_EXPORTER_OTLP_ENDPOINT=http://tempo:4317
ENV OTEL_EXPORTER_OTLP_PROTOCOL=grpc

# Environment variables for Pyroscope
ENV PYROSCOPE_APPLICATION_NAME=logiTrack
ENV PYROSCOPE_SERVER_ADDRESS=http://pyroscope:4040
ENV PYROSCOPE_FORMAT=jfr
ENV PYROSCOPE_PROFILER_EVENT=cpu
ENV PYROSCOPE_PROFILER_ALLOC=512k
ENV PYROSCOPE_UPLOAD_INTERVAL=10s

# Entrypoint with both agents
ENTRYPOINT ["java", \
    "-javaagent:/app/opentelemetry-javaagent.jar", \
    "-javaagent:/app/pyroscope.jar", \
    "-Xmx512m", \
    "-Xms256m", \
    "-jar", \
    "/app/logitrack.jar"]
