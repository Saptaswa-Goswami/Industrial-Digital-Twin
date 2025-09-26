# Digital Twin Service

## Overview

The Digital Twin Service maintains real-time state representations of all industrial machines, processes incoming sensor data, performs anomaly detection, and publishes state updates and alerts to downstream services. This service is the core of the predictive maintenance system.

## Architecture

### Core Components
1. **State Management Engine** - Maintains in-memory digital twin states
2. **Kafka Consumer** - Consumes machine telemetry events
3. **Anomaly Detection Engine** - Identifies potential issues
4. **State Persistence Manager** - Periodic state snapshots to database
5. **Metrics and Monitoring** - Performance and health tracking

### Project Structure
```
digital-twin/
├── src/main/java/com/industrial/digitaltwin/digitaltwin/
│   ├── DigitalTwinApplication.java
│   ├── config/
│   │   ├── KafkaConfig.java
│   │   ├── StateStoreConfig.java
│   │   ├── AnomalyDetectionConfig.java
│   │   └── WebSocketConfig.java
│   ├── model/
│   │   ├── DigitalTwinState.java
│   │   ├── MachineTelemetry.java
│   │   ├── AnomalyEvent.java
│   │   └── HistoricalDataPoint.java
│   ├── service/
│   │   ├── StateManagementService.java
│   │   ├── AnomalyDetectionService.java
│   │   ├── KafkaConsumerService.java
│   │   ├── StatePersistenceService.java
│   │   └── WebSocketBroadcastService.java
│   ├── repository/
│   │   └── StateSnapshotRepository.java
│   ├── controller/
│   │   └── DigitalTwinController.java
│   ├── websocket/
│   │   └── DigitalTwinWebSocketHandler.java
│   └── processor/
│       └── TelemetryProcessor.java
├── src/main/resources/
│   ├── application.yml
│   └── application-dev.yml
└── Dockerfile
```

## State Management

### DigitalTwinState.java
```java
public class DigitalTwinState {
    private String machineId;
    private Instant lastUpdated;
    private SensorData currentSensorData;
    private List<SensorData> historicalData; // Last N readings
    private Map<String, Object> computedMetrics; // Running averages, trends
    private MachineStatus status;
    private List<AnomalyRecord> recentAnomalies;
    private long operationalHours;
    private double efficiencyRating;
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

## WebSocket Real-time Updates

### WebSocket Configuration
- **Endpoint**: `/ws/machine-updates`
- **Message Format**: JSON with machine state updates
- **Broadcast Strategy**: Push updates to all connected clients when machine state changes
- **Session Management**: Track connected clients and handle connection lifecycle

### WebSocket Integration
- **Event-Driven Updates**: Automatically push state changes to connected WebSocket clients
- **Selective Broadcasting**: Option to broadcast to specific machine groups or users
- **Connection Monitoring**: Track active connections and handle disconnections gracefully
- **Performance Optimization**: Batch updates to prevent overwhelming clients with too frequent updates

### WebSocket Message Structure
```java
public class MachineStateUpdate {
    private String machineId;
    private Instant timestamp;
    private SensorData currentData;
    private MachineStatus status;
    private boolean isAnomalyDetected;
    private String machineType;
}
```

### State Store Strategy
- **In-Memory Cache**: ConcurrentHashMap for real-time access
- **Eviction Policy**: LRU-based on last access for memory management
- **Partitioning**: Distribute states based on machine ID for scalability

## Kafka Integration

### Topics Consumed
- `machine-telemetry` - Incoming sensor data from simulator

### Topics Produced
- `machine-state-updates` - State changes for downstream services
- `anomaly-events` - Anomaly detections for alert service

### Consumer Configuration
- Consumer groups for scalability
- Manual offset management for exactly-once processing
- Error handling with dead letter queue

## Anomaly Detection Implementation

### Multi-Level Detection
1. **Threshold-Based**: Simple high/low value detection
2. **Statistical**: Moving averages, standard deviations
3. **Pattern-Based**: Detect unusual patterns in sensor readings
4. **Predictive**: Compare current values to predicted values

### Anomaly Detection Service
```java
@Service
public class AnomalyDetectionService {
    
    public AnomalyEvent detectAnomalies(DigitalTwinState currentState, MachineTelemetry newTelemetry) {
        List<Anomaly> anomalies = new ArrayList<>();
        
        // Temperature anomaly detection
        anomalies.addAll(checkTemperatureAnomalies(currentState, newTelemetry));
        
        // Vibration anomaly detection
        anomalies.addAll(checkVibrationAnomalies(currentState, newTelemetry));
        
        // Load pattern anomalies
        anomalies.addAll(checkLoadAnomalies(currentState, newTelemetry));
        
        // Trend-based anomalies (degradation detection)
        anomalies.addAll(checkTrendAnomalies(currentState, newTelemetry));
        
        return !anomalies.isEmpty() ? new AnomalyEvent(currentState.getMachineId(), anomalies) : null;
    }
}
```

## Configuration Properties

### application.yml
```yaml
app:
  digital-twin:
    state-retention: 24h  # Keep historical data for 24 hours
    snapshot-interval: 5m  # Save state snapshots every 5 minutes
    anomaly-detection:
      temperature-threshold: 85.0
      vibration-threshold: 3.0
      load-threshold: 95.0
      statistical-deviation-multiplier: 2.5
    state-store:
      max-entries: 10000  # Maximum number of digital twins to track
      eviction-timeout: 1h  # Remove unused states after 1 hour

kafka:
  consumer:
    bootstrap-servers: kafka:9092
    group-id: digital-twin-group
    key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
    auto-offset-reset: latest
    enable-auto-commit: false
 producer:
    bootstrap-servers: kafka:9092
    key-serializer: org.apache.kafka.common.serialization.StringSerializer
    value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

## State Persistence Strategy

### Database Schema (PostgreSQL)
```sql
-- State snapshots for recovery
CREATE TABLE state_snapshots (
    id SERIAL PRIMARY KEY,
    machine_id VARCHAR(50) NOT NULL,
    snapshot_time TIMESTAMP NOT NULL,
    state_data JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Anomaly history for analysis
CREATE TABLE anomaly_history (
    id SERIAL PRIMARY KEY,
    machine_id VARCHAR(50) NOT NULL,
    anomaly_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    details JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Performance Optimization

### Memory Management
- Circular buffers for historical data to prevent memory leaks
- Weak references where appropriate
- Configurable state retention policies

### Processing Optimization
- Batch processing of multiple events
- Asynchronous state updates
- Parallel processing for different machines

## Health Checks and Monitoring

### Actuator Endpoints
- Health: Kafka connectivity, state store status
- Metrics: Processing rates, anomaly detection rates, memory usage
- Custom endpoints for state inspection

### Custom Metrics
- Events processed per second
- Anomalies detected per minute
- Average processing latency
- State store size and memory usage

## Error Handling and Resilience

### Circuit Breaker Pattern
- For database operations
- For downstream service calls
- Graceful degradation when components fail

### Retry Mechanisms
- Kafka producer retries with exponential backoff
- Database operation retries
- Circuit breaker fallbacks

## Future-Proofing Implementation

### Anomaly Detection Strategy Pattern
```java
public interface AnomalyDetectionStrategy {
    List<Anomaly> detect(DigitalTwinState currentState, MachineTelemetry newTelemetry);
}

@Component("threshold-based")
public class ThresholdAnomalyDetectionStrategy implements AnomalyDetectionStrategy {
    // Current implementation with threshold-based detection
}

@Component("ml-based")
@ConditionalOnProperty(name = "app.anomaly-detection.mode", havingValue = "ml-based")
public class MLAnomalyDetectionStrategy implements AnomalyDetectionStrategy {
    // Future implementation with ML-based detection
}

@Component("statistical")
@ConditionalOnProperty(name = "app.anomaly-detection.mode", havingValue = "statistical")
public class StatisticalAnomalyDetectionStrategy implements AnomalyDetectionStrategy {
    // Future implementation with statistical methods
}
```

### Configuration-Driven Behavior
```yaml
app:
  anomaly-detection:
    mode: "threshold-based"  # Can switch to "ml-based" or "statistical"
    threshold:
      enabled: true
    ml:
      enabled: false         # Enable when ready for ML
      model-path: "/models/"
    statistical:
      enabled: false         # Enable for statistical methods
```

## Implementation Phases

### Phase 1: Basic State Management
- Create basic Spring Boot application
- Implement Kafka consumer for telemetry
- Maintain simple in-memory state
- Publish basic state updates

### Phase 2: Anomaly Detection
- Implement threshold-based anomaly detection
- Add statistical anomaly detection
- Publish anomaly events to Kafka
- Add basic persistence

### Phase 3: Advanced Features
- Implement predictive anomaly detection
- Add historical data analysis
- Optimize performance and memory usage
- Add comprehensive monitoring

## Quality Assurance

### Code Quality Targets
- 85%+ test coverage
- Performance benchmarks
- Memory leak detection
- Integration testing

### Performance Targets
- Process 1000+ events per second per instance
- Sub-50ms processing latency
- Memory usage under 768MB for 10 machines
- 99.9% state consistency

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
COPY --from=build /app/target/digital-twin-*.jar app.jar

# Expose ports
EXPOSE 8080

# Optional: Add healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-Xms512m", "-Xmx768m", "-XX:+UseG1GC", "-jar", "/app.jar"]
```

## Success Criteria

1. **Functional**: Maintains accurate real-time states for all machines
2. **Reliable**: Processes all telemetry events with minimal data loss
3. **Performant**: Handles high-throughput event streams efficiently
4. **Intelligent**: Accurately detects anomalies with low false positive rate
5. **Resilient**: Recovers state from database after restart
6. **Observable**: Provides comprehensive operational metrics
7. **Future-Ready**: Architecture supports ML-based anomaly detection