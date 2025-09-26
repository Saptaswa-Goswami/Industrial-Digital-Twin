# Device Simulator Service

The Device Simulator Service generates realistic industrial machine sensor data and publishes it to Kafka topics. This service acts as the data source for the entire Industrial Equipment Digital Twin system, simulating 15 different types of industrial machines with various sensor readings.

## Overview

The service simulates the following 15 machine types:
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

## Architecture

### Core Components
1. **Machine Simulation Engine** - Generates sensor data for each machine type
2. **Kafka Producer** - Publishes events to Kafka topics
3. **Configuration Manager** - Handles simulation parameters
4. **Metrics Collector** - Tracks publishing statistics
5. **Health Monitor** - Ensures service availability

### Data Generation Strategy
- **Baseline Values**: Each machine type has specific baseline values for different sensors
- **Cyclical Patterns**: Diurnal, weekly, and seasonal variations
- **Operational States**: Idle, Normal Operation, Peak Load, Maintenance Mode
- **Anomaly Injection**: Gradual degradation, sudden failures, and intermittent issues

## Configuration

The service can be configured through the `application.yml` file:

```yaml
app:
  data-source:
    mode: "simulator"  # Can switch to "opc-ua" or "mqtt" for real machines
  simulation:
    sampling-interval: 5000  # milliseconds
    anomaly-probability: 0.05  # 5% chance of anomaly
    machines:
      # Configuration for each machine type
      - id: "PUMP_001"
        type: "centrifugal-pump"
        baseline-temp: 65.0
        temp-variation: 10.0
        baseline-vibration: 1.2
        vibration-variation: 0.8
        operational-pattern: "continuous"
```

## API Endpoints

- `GET /api/simulation/status` - Get simulation service status
- `GET /api/simulation/machines` - Get list of configured machines
- `GET /actuator/health` - Health check endpoint
- `GET /actuator/info` - Information endpoint
- `GET /actuator/metrics` - Metrics endpoint

## Kafka Integration

The service publishes machine telemetry data to the `machine-telemetry` topic with the following structure:

```json
{
  "machineId": "PUMP_001",
  "timestamp": "2025-01-01T10:00Z",
  "sensorData": {
    "temperature": 75.5,
    "vibration": 1.2,
    "load": 80.0,
    "pressure": 8.5,
    "rpm": 150
  },
 "status": "NORMAL",
  "schemaVersion": "1.0",
  "additionalMetrics": {},
  "dataSourceType": "simulator",
  "dataQualityScore": 1.0
}
```

## Future-Proofing Implementation

The service implements a data source strategy pattern that allows switching between simulation and real machine data sources:

- **Simulator Data Source**: Current implementation with simulation logic
- **OPC-UA Data Source**: Future implementation for real OPC-UA machines
- **MQTT Data Source**: Future implementation for real MQTT machines

## Running the Service

### Prerequisites
- Java 21
- Maven 3.6+
- Docker (for containerized deployment)
- Kafka cluster (for message publishing)

### Local Development
```bash
# Build the service
mvn clean package

# Run the service
mvn spring-boot:run
```

### Docker
```bash
# Build and run with Docker
docker build -t device-simulator .
docker run -p 8081:8081 device-simulator
```

### Docker Compose
The service is designed to work with the main docker-compose.yml file in the root directory.

## Monitoring and Health Checks

The service provides comprehensive monitoring through Spring Boot Actuator endpoints:
- Health: Kafka connectivity, simulation status
- Metrics: Message publishing rate, error counts
- Info: Simulation configuration details

## Error Handling

- Kafka connection retry logic
- Dead letter queue for failed messages
- Circuit breaker pattern for resilience
- Comprehensive logging

## Testing

The service includes comprehensive unit and integration tests covering:
- Simulation algorithm validation
- Kafka producer methods
- Configuration loading
- Data generation logic