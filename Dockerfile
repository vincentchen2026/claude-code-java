# Claude Code Java — Multi-stage Docker build
# Stage 1: Build
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /build
COPY pom.xml .
COPY claude-code-utils/pom.xml claude-code-utils/
COPY claude-code-core/pom.xml claude-code-core/
COPY claude-code-api/pom.xml claude-code-api/
COPY claude-code-permissions/pom.xml claude-code-permissions/
COPY claude-code-tools/pom.xml claude-code-tools/
COPY claude-code-commands/pom.xml claude-code-commands/
COPY claude-code-mcp/pom.xml claude-code-mcp/
COPY claude-code-bridge/pom.xml claude-code-bridge/
COPY claude-code-session/pom.xml claude-code-session/
COPY claude-code-services/pom.xml claude-code-services/
COPY claude-code-ui/pom.xml claude-code-ui/
COPY claude-code-cli/pom.xml claude-code-cli/
COPY claude-code-app/pom.xml claude-code-app/

# Download dependencies first (cached layer)
RUN apt-get update && apt-get install -y maven && \
    mvn dependency:go-offline -B || true

# Copy source and build
COPY . .
RUN mvn package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
COPY --from=builder /build/claude-code-app/target/claude-code-app-*.jar app.jar

# Create non-root user
RUN groupadd -r claude && useradd -r -g claude claude
USER claude

ENTRYPOINT ["java", "-jar", "app.jar"]
