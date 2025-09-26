# Service Incremental Testing Strategy

## Overview

When implementing the Industrial Equipment Digital Twin system incrementally, services must be tested individually before the complete system is operational. This document outlines strategies for testing each service as it's developed, addressing the challenge of event-driven communication when not all services are available.

## Testing Approach by Service

### 1. Device Simulator Service Testing

The Device Simulator Service is the first to be implemented and only produces messages to Kafka, making it the easiest to test initially.

#### Testing Strategies:
- **Unit Testing**: Test the simulation algorithms and data generation logic independently
- **Kafka Producer Testing**: Verify messages are published to the correct topics with proper schema
- **Manual Verification**: Use Kafka tools to verify messages are being produced
- **Load Testing**: Test message throughput and system performance

#### Testing Commands:
```bash
# Verify Kafka topics are receiving messages
docker exec kafka-digital-twin kafka-console-consumer --bootstrap-server localhost:9092 --topic machine-telemetry --from-beginning

# Check topic details
docker exec kafka-digital-twin kafka-topics --bootstrap-server localhost:9092 --describe --topic machine-telemetry
```

### 2. Digital Twin Service Testing

The Digital Twin Service is the second to be implemented and consumes from Kafka. Since the Device Simulator may not be running continuously during development, alternative testing strategies are needed.

#### Testing Strategies:
- **Mock Data Testing**: Create test data files with realistic machine telemetry
- **Manual Message Publishing**: Publish test messages directly to Kafka topics
- **Integration Testing**: Once Device Simulator is available, test the complete flow
- **State Validation**: Verify state management logic with known input data

#### Testing Commands:
```bash
# Publish test messages manually to Kafka
echo '{"machineId":"TEST_MACHINE_001","timestamp":"2025-09-25T10:00:00Z","sensorData":{"temperature":75.0,"vibration":1.5,"load":80.0,"pressure":8.5,"rpm":1500}}' | docker exec -i kafka-digital-twin kafka-console-producer --bootstrap-server localhost:9092 --topic machine-telemetry

# Verify Digital Twin is consuming and processing
docker exec kafka-digital-twin kafka-console-consumer --bootstrap-server localhost:9092 --topic machine-state-updates --from-beginning
```

### 3. Alert/Analytics Service Testing

The Alert/Analytics Service is the third to be implemented and consumes from multiple Kafka topics. Testing requires careful planning since it depends on other services producing data.

#### Testing Strategies:
- **Synthetic Event Testing**: Create artificial anomaly events to test alert processing
- **Database Validation**: Verify analytics and alert storage in the database
- **Notification Testing**: Test notification delivery with test configurations
- **Integration Testing**: Test with real data once all services are available

## Comprehensive Testing Strategies

### 1. Isolated Service Testing

#### For Producers (Device Simulator):
- Test message schema validation
- Verify partitioning strategy
- Test error handling and retry mechanisms
- Validate message throughput

#### For Consumers (Digital Twin, Alert/Analytics):
- Test message deserialization
- Validate business logic with sample data
- Test error handling for malformed messages
- Verify consumer group behavior

### 2. Synthetic Data Generation

Create synthetic data sets for testing purposes:

```java
// Example synthetic data generator
@Component
public class SyntheticDataGenerator {
    
    public MachineTelemetry generateNormalTelemetry(String machineId) {
        return MachineTelemetry.builder()
            .machineId(machineId)
            .timestamp(Instant.now())
            .sensorData(SensorData.builder()
                .temperature(70.0 + (Math.random() * 10)) // Normal range
                .vibration(1.0 + (Math.random() * 0.5))
                .load(70.0 + (Math.random() * 20))
                .pressure(8.0 + (Math.random() * 2))
                .rpm(1400 + (int)(Math.random() * 200))
                .build())
            .build();
    }
    
    public MachineTelemetry generateAnomalyTelemetry(String machineId) {
        return MachineTelemetry.builder()
            .machineId(machineId)
            .timestamp(Instant.now())
            .sensorData(SensorData.builder()
                .temperature(90.0 + (Math.random() * 10)) // High temperature anomaly
                .vibration(3.0 + (Math.random() * 2))
                .load(95.0)
                .pressure(12.0)
                .rpm(1600)
                .build())
            .build();
    }
}
```

### 3. Kafka Testing Utilities

#### Topic Management:
```bash
# Create topics manually if auto-creation is disabled
docker exec kafka-digital-twin kafka-topics --create --topic machine-telemetry --bootstrap-server localhost:9092 --partitions 3
docker exec kafka-digital-twin kafka-topics --create --topic machine-state-updates --bootstrap-server localhost:9092 --partitions 3
docker exec kafka-digital-twin kafka-topics --create --topic anomaly-events --bootstrap-server localhost:9092 --partitions 3

# List all topics
docker exec kafka-digital-twin kafka-topics --list --bootstrap-server localhost:9092
```

#### Message Testing:
```bash
# Produce test messages with specific keys
echo '{"machineId":"PUMP_001","timestamp":"2025-09-25T10:00:00Z","sensorData":{"temperature":75.0,"vibration":1.5,"load":80.0}}' | docker exec -i kafka-digital-twin kafka-console-producer --bootstrap-server localhost:9092 --topic machine-telemetry --property "key.serializer=org.apache.kafka.common.serialization.StringSerializer"

# Consume messages with keys
docker exec kafka-digital-twin kafka-console-consumer --bootstrap-server localhost:9092 --topic machine-telemetry --from-beginning --property "key.deserializer=org.apache.kafka.common.serialization.StringDeserializer"
```

### 4. Service Health and Monitoring

#### Health Checks:
- Implement comprehensive health indicators for Kafka connectivity
- Monitor consumer lag when consuming services are implemented
- Track message processing rates
- Monitor database connectivity for services that use it

#### Example Health Indicator:
```java
@Component
public class KafkaHealthIndicator implements HealthIndicator {
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Override
    public Health health() {
        try {
            // Test Kafka connectivity
            kafkaTemplate.send("health-check", "health-test", "ping");
            Health.up()
                .withDetail("kafka", "Available")
                .withDetail("timestamp", Instant.now())
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("kafka", "Unavailable")
                .withException(e)
                .build();
        }
    }
}
```

### 5. Integration Testing Pipeline

#### Phase 1: Single Service Testing
- Test service in isolation
- Validate configuration and dependencies
- Verify basic functionality

#### Phase 2: Two-Service Integration
- Deploy two services together
- Test event flow between them
- Validate data transformation and processing

#### Phase 3: Complete System Testing
- Deploy all services
- Test end-to-end functionality
- Perform load and performance testing

### 6. Testing Validation Checklist

#### For Device Simulator Service:
- [ ] Messages are published to correct Kafka topic
- [ ] Message schema is valid and consistent
- [ ] Multiple machine types generate different data patterns
- [ ] Anomaly injection works when enabled
- [ ] Service handles Kafka connection issues gracefully

#### For Digital Twin Service:
- [ ] Successfully consumes from Kafka topics
- [ ] State management works correctly
- [ ] Anomaly detection algorithms function properly
- [ ] State updates are published to correct topics
- [ ] Database persistence works as expected

#### For Alert/Analytics Service:
- [ ] Successfully consumes from all required Kafka topics
- [ ] Alert generation logic works correctly
- [ ] Notifications are sent via configured channels
- [ ] Analytics calculations are accurate
- [ ] Database operations function properly

### 7. Troubleshooting Common Issues

#### Kafka Connection Issues:
- Verify Kafka is running and accessible
- Check bootstrap server configuration
- Validate network connectivity between services

#### Message Schema Issues:
- Ensure message format matches expected schema
- Validate JSON serialization/deserialization
- Check for version compatibility issues

#### Consumer Group Issues:
- Verify consumer group configuration
- Check for rebalancing issues
- Monitor consumer lag

This incremental testing approach ensures each service can be validated individually before integrating with the complete system, addressing the challenge of testing event-driven communication when not all services are available.