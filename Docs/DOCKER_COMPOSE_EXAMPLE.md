# Centralized Docker Compose Configuration

This document illustrates the structure of the centralized `docker-compose.yml` file that orchestrates all services, Kafka, and databases for the Industrial Equipment Digital Twin system.

## Complete docker-compose.yml Structure

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
      KAFKA_LISTENERS: "PLAINTEXT://0.0.0:9092,CONTROLLER://0.0.0:9093"
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
      - "5433:5432"
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

  # Device Simulator Service (Added when implemented)
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
    ports:
      - "8081:8080"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    mem_limit: 512m
    restart: unless-stopped

  # Digital Twin Service (Added when implemented)
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

  # Alert/Analytics Service (Added when implemented)
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

## Key Features of the Centralized Orchestration

### 1. Infrastructure Components
- **Kafka**: Configured in KRaft mode without Zookeeper
- **PostgreSQL Databases**: Separate databases for Digital Twin and Alert/Analytics services
- **Health Checks**: Ensures services are ready before dependent services start

### 2. Service Integration
- **Centralized Configuration**: All services defined in a single file
- **Dependency Management**: Proper `depends_on` declarations ensure correct startup order
- **Environment Variables**: Centralized configuration management
- **Resource Management**: Memory limits and restart policies

### 3. Implementation Workflow
As each service is implemented:

1. **Initial State**: docker-compose.yml contains only infrastructure (Kafka and databases)
2. **Device Simulator**: Added first with dependency on Kafka
3. **Digital Twin**: Added with dependencies on Kafka and its database
4. **Alert/Analytics**: Added with dependencies on Kafka and its database

### 4. Service Addition Process
When adding each service to the docker-compose.yml:

- Add the service definition with proper image/context configuration
- Set up environment variables for service configuration
- Define dependencies using `depends_on` with health checks
- Configure ports for external access
- Set up health checks and restart policies
- Define memory limits and other resource constraints

This centralized approach ensures consistent configuration management and proper service orchestration across the entire Industrial Equipment Digital Twin system.