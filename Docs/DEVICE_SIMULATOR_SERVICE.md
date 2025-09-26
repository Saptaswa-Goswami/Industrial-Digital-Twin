# Device Simulator Service

## Overview

The Device Simulator Service generates realistic industrial machine sensor data and publishes it to Kafka topics. This service acts as the data source for the entire system, simulating 15 different types of industrial machines with various sensor readings.

## Architecture

### Core Components
1. **Machine Simulation Engine** - Generates sensor data for each machine type
2. **Kafka Producer** - Publishes events to Kafka topics
3. **Configuration Manager** - Handles simulation parameters
4. **Metrics Collector** - Tracks publishing statistics
5. **Health Monitor** - Ensures service availability

### Project Structure
```
device-simulator/
├── src/main/java/com/industrial/digitaltwin/devicesimulator/
│   ├── DeviceSimulatorApplication.java
│   ├── config/
│   │   ├── KafkaConfig.java
│   │   └── SimulationConfig.java
│   ├── model/
│   │   ├── MachineData.java
│   │   ├── SensorData.java
│   │   └── MachineState.java
│   ├── service/
│   │   ├── MachineSimulationService.java
│   │   ├── AnomalyInjectionService.java
│   │   └── KafkaProducerService.java
│   ├── controller/
│   │   └── SimulationController.java
│   └── scheduler/
│       └── DataGenerationScheduler.java
├── src/main/resources/
│   ├── application.yml
│   └── application-dev.yml
└── Dockerfile
```

## Machine Types and Data Generation

### 15 Different Machine Types
1. Centrifugal Pump
2. Rotary Compressor
3. Industrial Motor (AC/DC)
4. Conveyor Belt System
5. Gearbox
6. Heat Exchanger
7. Steam Turbine
8. Cooling Tower
9. Industrial Fan
10. Hydraulic Press
11. Boiler System
12. Rotary Kiln
13. Crane System
14. Cooling Pump
15. Reciprocating Compressor

### Data Generation Strategy

#### Baseline Values
- Each machine type has specific baseline values for different sensors
- Values fluctuate within realistic ranges based on machine type

#### Cyclical Patterns
- Diurnal patterns (day/night operational differences)
- Weekly patterns (weekend vs weekday usage)
- Seasonal variations (temperature, load changes)

#### Operational States
- Idle: Lower baseline values
- Normal Operation: Standard operating ranges
- Peak Load: Higher values within safe limits
- Maintenance Mode: Reduced operational parameters

#### Anomaly Injection
- Gradual degradation over time (bearing wear, efficiency loss)
- Sudden failures (overheating, excessive vibration)
- Intermittent issues (loose connections, partial blockages)

## Configuration Properties

### application.yml
```yaml
app:
  simulation:
    machines:
      - id: "PUMP_001"
        type: "centrifugal-pump"
        baseline-temp: 65.0
        temp-variation: 10.0
        baseline-vibration: 1.2
        vibration-variation: 0.8
        operational-pattern: "continuous"
      - id: "COMPRESSOR_001"
        type: "rotary-compressor"
        baseline-temp: 75.0
        temp-variation: 15.0
        baseline-vibration: 1.8
        vibration-variation: 1.2
        operational-pattern: "intermittent"
    sampling-interval: 5000  # milliseconds
    anomaly-probability: 0.05  # 5% chance of anomaly
    machine-types: ["pump", "motor", "conveyor", "compressor"]

kafka:
  producer:
    bootstrap-servers: kafka:9092
    key-serializer: org.apache.kafka.common.serialization.StringSerializer
    value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    acks: 1
    retries: 3
    batch-size: 16384
    linger-ms: 5
    buffer-memory: 33554432
```

## Data Models

### MachineData.java
```java
public class MachineData {
    private String machineId;
    private Instant timestamp;
    private SensorData sensorData;
    private MachineStatus status;
    private Map<String, Object> additionalMetrics;
}

public class SensorData {
    private double temperature;
    private double vibration;
    private double load;
    private double pressure;
    private double rpm;
}

### MachineStatus Enum (Unified across all services)
```java
public enum MachineStatus {
    NORMAL,      // Normal operation
    IDLE,        // Machine is idle
    PEAK_LOAD,   // Machine operating at peak load
    WARNING,     // Warning state - requires attention
    CRITICAL,    // Critical state - immediate action required
    MAINTENANCE, // Machine in maintenance mode
    OFFLINE,     // Machine is offline
    ERROR        // Machine in error state
}
```

## Kafka Integration

### Topics
- `machine-telemetry` - Main sensor data stream

### Producer Configuration
- JSON serialization
- Proper partitioning strategy
- Error handling and retry mechanisms
- Asynchronous publishing with callbacks

## Future-Proofing Implementation

### Data Source Strategy Pattern
```java
public interface MachineDataSource {
    Flux<MachineTelemetry> getMachineDataFlux();
    void initialize();
    void shutdown();
}

@Component("simulator")
public class SimulatorMachineDataSource implements MachineDataSource {
    // Current implementation with simulation logic
}

@Component("opc-ua")
@ConditionalOnProperty(name = "app.data-source.mode", havingValue = "opc-ua")
public class OpcUaMachineDataSource implements MachineDataSource {
    // Future implementation for real OPC-UA machines
}

@Component("mqtt")
@ConditionalOnProperty(name = "app.data-source.mode", havingValue = "mqtt")
public class MqttMachineDataSource implements MachineDataSource {
    // Future implementation for real MQTT machines
}
```

### Configuration-Driven Behavior
```yaml
app:
  data-source:
    mode: "simulator"  # Can switch to "opc-ua" or "mqtt" for real machines
    simulation:
      enabled: true
      machines-count: 15
    real-machines:
      enabled: false    # Enable when ready for real integration
      protocol: "opc-ua"  # opc-ua, mqtt, modbus, etc.
```

## Scheduling and Execution

### Data Generation Scheduler
- Use `@Scheduled` with configurable interval
- Parallel processing for multiple machines
- Graceful shutdown handling
- Performance monitoring

## Health Checks and Monitoring

### Actuator Endpoints
- Health: Kafka connectivity, simulation status
- Metrics: Message publishing rate, error counts
- Info: Simulation configuration details

### Custom Health Indicator
- Check Kafka producer connectivity
- Verify message publishing capability

## Error Handling

### Robust Error Management
- Kafka connection retry logic
- Dead letter queue for failed messages
- Circuit breaker pattern for resilience
- Comprehensive logging

## Testing Strategy

### Unit Tests
- Simulation algorithm validation
- Data generation logic
- Kafka producer methods

### Integration Tests
- Kafka connectivity
- Message publishing verification
- Configuration validation

### Load Testing
- Simulate hundreds of machines
- Verify message throughput
- Memory and CPU usage monitoring

## Quality Assurance

### Code Quality Targets
- 80%+ test coverage
- Static code analysis
- Performance benchmarks
- Security scanning

### Performance Targets
- Support 100+ machines with 1-5 second intervals
- Sub-100ms message publishing latency
- Memory usage under 512MB
- 99.9% message publishing success rate

## Deployment Configuration

### Dockerfile (Multi-stage)
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

## Success Criteria

1. **Functional**: Successfully publishes realistic machine data to Kafka
2. **Reliable**: Maintains consistent message publishing with minimal errors
3. **Scalable**: Handles configurable number of machines efficiently
4. **Observable**: Provides comprehensive metrics and health information
5. **Robust**: Handles failures gracefully with proper recovery
6. **Future-Ready**: Architecture supports real machine integration