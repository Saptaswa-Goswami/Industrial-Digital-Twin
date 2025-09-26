# Architecture Overview

## System Architecture

The Industrial Equipment Digital Twin system follows a microservices architecture with event-driven communication patterns. The system consists of three main services that communicate through Apache Kafka topics.

## High-Level Architecture

```
┌─────────────────────┐    Kafka Topics     ┌─────────────────────┐    Kafka Topics     ┌─────────────────────┐
│                     │ ────────────────→   │                     │ ────────────────→   │                     │
│  Device Simulator   │                     │  Digital Twin       │                     │  Alert/Analytics    │
│                     │ ←──────────────     │                     │ ←──────────────     │                     │
│  Service            │   State Updates     │  Service            │   Anomaly Events    │  Service            │
│                     │                     │                     │
└─────────────────────┘                     └─────────────────────┘                     └─────────────────────┘
```

## Service Responsibilities

### Device Simulator Service
- Generates realistic industrial machine sensor data
- Simulates 15 different types of industrial machines (pumps, compressors, motors, etc.)
- Publishes machine telemetry data to Kafka topics
- Configurable simulation parameters for different scenarios

### Digital Twin Service
- Consumes machine telemetry from Kafka
- Maintains real-time state representations (digital twins) for each machine
- Performs real-time anomaly detection on incoming sensor data
- Publishes state updates and anomaly events to downstream services
- Maintains historical data for analysis and replay capabilities

### Alert/Analytics Service
- Consumes anomaly events and state updates from Kafka
- Generates maintenance alerts based on detected anomalies
- Sends notifications via multiple channels (email, SMS)
- Performs analytics on machine performance and trends
- Generates maintenance reports and insights

## Communication Patterns

### Event-Driven Architecture
- All services communicate exclusively through Kafka topics
- No direct service-to-service calls
- Asynchronous, non-blocking communication
- Complete decoupling between services

### Kafka Topics
1. `machine-telemetry` - Raw sensor data from devices
2. `machine-state-updates` - Updated machine states from digital twin
3. `anomaly-events` - Detected anomalies from digital twin
4. `alerts` - Generated alerts for further processing

## Data Flow

1. Device Simulator generates sensor data for 15 machines every few seconds
2. Data is published to `machine-telemetry` topic in Kafka
3. Digital Twin Service consumes telemetry data and updates digital twin states
4. Anomaly detection algorithms run on incoming data
5. If anomalies are detected, events are published to `anomaly-events` topic
6. State updates are published to `machine-state-updates` topic
7. Alert/Analytics Service consumes both anomaly events and state updates
8. Alerts are generated and notifications are sent
9. Analytics are computed and reports are generated

## Communication Patterns

### REST API Communication
- Clients access service endpoints for on-demand data retrieval
- Synchronous request-response pattern
- Suitable for historical data queries and configuration

### WebSocket Communication
- Real-time, bidirectional communication channels
- Push-based updates from services to connected clients
- Low-latency updates for live monitoring

#### Digital Twin Service WebSocket:
- **Endpoint**: `/ws/machine-updates`
- **Purpose**: Real-time machine state updates
- **Message Type**: Machine state changes and status updates

#### Alert/Analytics Service WebSocket:
- **Endpoint**: `/ws/alert-updates`
- **Purpose**: Real-time alert and analytics notifications
- **Message Type**: New alerts, analytics updates, and reports

## Scalability Considerations

- Kafka partitions enable horizontal scaling for handling hundreds of machines
- Consumer groups allow parallel processing
- Services can be scaled independently based on load
- State management is distributed across service instances

## Fault Tolerance

- Kafka provides durability and replay capabilities
- Services are designed to be stateless where possible
- Circuit breaker patterns protect against cascading failures
- Dead letter queues handle processing failures

## Data Model Consistency

When implementing new services (like the Alert/Analytics service), ensure the following data models are consistent across all services to avoid deserialization issues:

### MachineStatus Enum (Unified)
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

### MachineTelemetry Model
```java
public class MachineTelemetry {
    private String machineId;
    private Instant timestamp;
    private SensorData sensorData;
    private MachineStatus status;
    private String schemaVersion = "1.0";
    private String dataSourceType = "simulator";
    private Map<String, Object> additionalMetrics = new HashMap<>();
    private double dataQualityScore = 1.0;
}
```

### SensorData Model
```java
public class SensorData {
    private double temperature;
    private double vibration;
    private double load;
    private double pressure;
    private double rpm;
}
```

## Service Implementation Guidelines

When implementing new services like the Alert/Analytics service, follow these guidelines to avoid deserialization issues:

1. **Use consistent data models**: Implement the same data models across all services
2. **Configure Kafka properly**: Set `JsonSerializer.ADD_TYPE_INFO_HEADERS = false` in producers to avoid classpath issues
3. **Use ErrorHandlingDeserializer**: Configure consumers with ErrorHandlingDeserializer for resilience
4. **Map status values appropriately**: Ensure status conversion between services follows the unified enum
5. **Define trusted packages**: Include all service model packages in the trusted packages list
6. **Deserialize to Map first**: For maximum compatibility, deserialize to `Map<String, Object>` and then convert to internal models

## Future-Proofing Architecture

- Strategy pattern for different anomaly detection algorithms
- Adapter pattern for various data source integrations
- Configuration-driven behavior switching
- Interface-based design for easy extensibility