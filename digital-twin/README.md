# Digital Twin Service

The Digital Twin Service maintains real-time state representations of all industrial machines, processes incoming sensor data, performs anomaly detection, and publishes state updates and alerts to downstream services. This service is the core of the predictive maintenance system.

## Overview

The Digital Twin Service:
- Maintains in-memory digital twin states for all machines
- Consumes machine telemetry from Kafka topics
- Performs real-time anomaly detection on incoming sensor data
- Publishes state updates and anomaly events to downstream services
- Maintains historical data for analysis and replay capabilities
- Provides REST API endpoints for state inspection
- Offers WebSocket endpoints for real-time updates

## Architecture

### Core Components
1. **State Management Engine** - Maintains in-memory digital twin states
2. **Kafka Consumer** - Consumes machine telemetry events
3. **Anomaly Detection Engine** - Identifies potential issues
4. **State Persistence Manager** - Periodic state snapshots to database
5. **Metrics and Monitoring** - Performance and health tracking

### Data Flow
1. Consumer receives telemetry data from `machine-telemetry` Kafka topic
2. State is updated in-memory with new telemetry
3. Anomaly detection algorithms run on incoming data
4. If anomalies are detected, events are published to `anomaly-events` topic
5. State updates are published to `machine-state-updates` topic
6. WebSocket updates are broadcast to connected clients

## Configuration

The service is configured via `application.yml` with the following key properties:

```yaml
app:
  digital-twin:
    state-retention: 24h  # Keep historical data for 24 hours
    snapshot-interval: 5m  # Save state snapshots every 5 minutes
    anomaly-detection:
      mode: "threshold-based"  # Can switch to "ml-based" or "statistical"
      temperature-threshold: 85.0
      vibration-threshold: 3.0
      load-threshold: 95.0
      statistical-deviation-multiplier: 2.5
    state-store:
      max-entries: 10000  # Maximum number of digital twins to track
      eviction-timeout: 1h  # Remove unused states after 1 hour
```

## Endpoints

### REST API
- `GET /api/digital-twin/state/{machineId}` - Get current state of a specific machine
- `GET /api/digital-twin/states` - Get states of all machines
- `DELETE /api/digital-twin/state/{machineId}` - Clear state of a specific machine

### WebSocket
- `/ws/machine-updates` - Real-time machine state updates

## Anomaly Detection Strategies

The service implements a strategy pattern for anomaly detection:
- **Threshold-based** (default): Simple high/low value detection
- **Statistical**: Moving averages, standard deviations (future)
- **ML-based**: Machine learning models (future)

## Kafka Topics

### Consumed
- `machine-telemetry` - Incoming sensor data from simulator

### Produced
- `machine-state-updates` - State changes for downstream services
- `anomaly-events` - Anomaly detections for alert service

## Future-Proofing

The service is designed with extensibility in mind:
- Strategy pattern for different anomaly detection algorithms
- Configuration-driven behavior switching
- Interface-based design for easy extensibility
- JSONB fields in database for flexible data storage
- Schema versioning for data models

## Running the Service

### Prerequisites
- Java 21
- Maven 3.9+
- Docker (for containerized deployment)

### Local Development
```bash
# Build the service
mvn clean install

# Run with Maven
mvn spring-boot:run

# Or run the JAR
java -jar target/digital-twin-0.0.1-SNAPSHOT.jar
```

### Docker
```bash
# Build and run with Docker
docker build -t digital-twin .
docker run -p 8082:8082 digital-twin
```

## Health Checks and Monitoring

The service includes Spring Boot Actuator endpoints:
- `/actuator/health` - Service health status
- `/actuator/info` - Application information
- `/actuator/metrics` - Performance metrics
- `/actuator/health/liveness` - Liveness probe
- `/actuator/health/readiness` - Readiness probe