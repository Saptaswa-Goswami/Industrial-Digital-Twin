# Docker Configuration

## Overview

The Industrial Equipment Digital Twin system uses Docker for containerization and Docker Compose for orchestration. All services follow multi-stage build patterns with Java 21 and are optimized for production deployment.

## Multi-Stage Dockerfiles

### Device Simulator Service Dockerfile
```dockerfile
# -------------------------
# Stage 1: Build
# -------------------------
FROM maven:3.9.4-eclipse-temurin-21 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (cacheable)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# -------------------------
# Stage 2: Runtime
# -------------------------
FROM eclipse-temurin:21-jre-jammy

# Create a non-root user for security
RUN useradd -ms /bin/bash appuser
USER appuser

# Set working directory
WORKDIR /app

# Copy the JAR from build stage
COPY --from=build /app/target/device-simulator-*.jar app.jar

# Expose ports
EXPOSE 8080

# Optional: Add healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-XX:+UseG1GC", "-jar", "/app.jar"]
```

### Digital Twin Service Dockerfile
```dockerfile
# -------------------------
# Stage 1: Build
# -------------------------
FROM maven:3.9.4-eclipse-temurin-21 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (cacheable)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# -------------------------
# Stage 2: Runtime
# -------------------------
FROM eclipse-temurin:21-jre-jammy

# Create a non-root user for security
RUN useradd -ms /bin/bash appuser
USER appuser

# Set working directory
WORKDIR /app

# Copy the JAR from build stage
COPY --from=build /app/target/digital-twin-*.jar app.jar

# Expose ports
EXPOSE 8080

# Optional: Add healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-Xms512m", "-Xmx768m", "-XX:+UseG1GC", "-jar", "/app.jar"]
```

### Alert/Analytics Service Dockerfile
```dockerfile
# -------------------------
# Stage 1: Build
# -------------------------
FROM maven:3.9.4-eclipse-temurin-21 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (cacheable)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# -------------------------
# Stage 2: Runtime
# -------------------------
FROM eclipse-temurin:21-jre-jammy

# Create a non-root user for security
RUN useradd -ms /bin/bash appuser
USER appuser

# Set working directory
WORKDIR /app

# Copy the JAR from build stage
COPY --from=build /app/target/alert-analytics-*.jar app.jar

# Expose ports
EXPOSE 8080

# Optional: Add healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-Xms384m", "-Xmx600m", "-XX:+UseG1GC", "-jar", "/app.jar"]
```

## Docker Compose Configuration

### docker-compose.yml
```yaml
version: '3.8'

services:
  # Kafka using KRaft mode (no Zookeeper)
  kafka:
    image: confluentinc/cp-kafka:7.6.1
    container_name: kafka-digital-twin
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: "broker,controller"
      KAFKA_CONTROLLER_QUORUM_VOTERS: "1@kafka:9093"
      KAFKA_LISTENERS: "PLAINTEXT://0.0.0:9092,CONTROLLER://0.0.0.0:9093"
      KAFKA_CONTROLLER_LISTENER_NAMES: "CONTROLLER"
      KAFKA_INTER_BROKER_LISTENER_NAME: "PLAINTEXT"
      KAFKA_BROKER_ID: 1
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_LOG_FLUSH_INTERVAL_MESSAGES: 9223372036854775807
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
      KAFKA_HEAP_OPTS: "-Xms512m -Xmx1g"
    volumes:
      - kafka_data:/var/lib/kafka/data
    healthcheck:
      test: ["CMD-SHELL", "kafka-broker-api-versions --bootstrap-server localhost:9092"]
      interval: 30s
      timeout: 10s
      retries: 5
    mem_limit: 1.2g

  # PostgreSQL for Digital Twin Service
  postgres-digital-twin:
    image: postgres:15
    container_name: postgres-digital-twin
    environment:
      POSTGRES_DB: digital_twin
      POSTGRES_USER: digital_twin_user
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-password123}
    volumes:
      - postgres_digital_twin_data:/var/lib/postgresql/data
      - ./init-db-digital-twin.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "543:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U digital_twin_user -d digital_twin"]
      interval: 10s
      timeout: 5s
      retries: 5
    mem_limit: 512m

  # PostgreSQL for Alert/Analytics Service
  postgres-alert-analytics:
    image: postgres:15
    container_name: postgres-alert-analytics
    environment:
      POSTGRES_DB: alert_analytics
      POSTGRES_USER: alert_analytics_user
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-password123}
    volumes:
      - postgres_alert_analytics_data:/var/lib/postgresql/data
      - ./init-db-alert-analytics.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "5434:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U alert_analytics_user -d alert_analytics"]
      interval: 10s
      timeout: 5s
      retries: 5
    mem_limit: 512m

  # Device Simulator Service
 device-simulator:
    build:
      context: ./device-simulator
      dockerfile: Dockerfile
    container_name: device-simulator
    depends_on:
      kafka:
        condition: service_healthy
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-digital-twin:5432/digital_twin
      - SPRING_DATASOURCE_USERNAME=digital_twin_user
      - SPRING_DATASOURCE_PASSWORD=${POSTGRES_PASSWORD:-password123}
    ports:
      - "8081:8080"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    mem_limit: 512m
    restart: unless-stopped

  # Digital Twin Service
  digital-twin:
    build:
      context: ./digital-twin
      dockerfile: Dockerfile
    container_name: digital-twin
    depends_on:
      kafka:
        condition: service_healthy
      postgres-digital-twin:
        condition: service_healthy
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-digital-twin:5432/digital_twin
      - SPRING_DATASOURCE_USERNAME=digital_twin_user
      - SPRING_DATASOURCE_PASSWORD=${POSTGRES_PASSWORD:-password123}
    ports:
      - "8082:8080"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    mem_limit: 768m
    restart: unless-stopped

  # Alert/Analytics Service
  alert-analytics:
    build:
      context: ./alert-analytics
      dockerfile: Dockerfile
    container_name: alert-analytics
    depends_on:
      kafka:
        condition: service_healthy
      postgres-alert-analytics:
        condition: service_healthy
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-alert-analytics:5432/alert_analytics
      - SPRING_DATASOURCE_USERNAME=alert_analytics_user
      - SPRING_DATASOURCE_PASSWORD=${POSTGRES_PASSWORD:-password123}
    ports:
      - "8083:8080"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    mem_limit: 600m
    restart: unless-stopped

volumes:
  kafka_data:
  postgres_digital_twin_data:
  postgres_alert_analytics_data:
```

## Environment Configuration

### .env File
```env
# Database passwords
POSTGRES_PASSWORD=your_secure_password_here

# Kafka configuration
KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092

# Application configuration
SPRING_PROFILES_ACTIVE=docker

# Notification service credentials (for future use)
TWILIO_ACCOUNT_SID=your_twilio_account_sid
TWILIO_AUTH_TOKEN=your_twilio_auth_token
TWILIO_PHONE_NUMBER=your_twilio_phone_number

# SMTP configuration for email notifications
EMAIL_USERNAME=your_email_username
EMAIL_PASSWORD=your_email_password
```

## Build and Deployment Instructions

### Local Development
```bash
# Build and start all services
docker-compose up --build

# Start specific service
docker-compose up --build digital-twin

# View logs
docker-compose logs -f device-simulator
docker-compose logs -f digital-twin
docker-compose logs -f alert-analytics
```

### Production Deployment
```bash
# Use production-specific compose file
docker-compose -f docker-compose.prod.yml up -d

# Scale services
docker-compose up --scale digital-twin=3
```

## Resource Management

### Memory Limits
- Kafka: 1.2GB (KRaft mode optimized)
- Device Simulator: 512MB
- Digital Twin: 768MB (higher for state management)
- Alert/Analytics: 600MB
- PostgreSQL (each): 512MB

### CPU Configuration
- All services configured with default CPU shares
- Can be adjusted based on load requirements

## Security Considerations

### Non-Root Execution
- All application containers run as non-root users
- Reduces potential security impact

### Network Isolation
- Docker Compose creates isolated network
- Services communicate only through defined connections

### Secret Management
- Environment variables for sensitive data
- External secret management for production

## Health Checks and Monitoring

### Service Health Checks
- All services include health check endpoints
- Docker monitors service availability
- Automatic restart on failure

### Kafka Health Monitoring
- Built-in health checks for Kafka broker
- Verifies broker API availability
- Ensures proper startup sequence

## Future-Ready Configuration

### Service Scaling
- Configured for easy horizontal scaling
- Consumer groups support multiple instances
- Database connections optimized for scaling

### Configuration Flexibility
- Environment-based configuration
- Profile-specific settings
- Easy deployment to different environments