# Industrial Equipment Digital Twin – Predictive Maintenance

## Project Overview

This project implements a backend-only, event-driven system that simulates industrial machinery, tracks their state in real-time, detects anomalies, and triggers alerts for predictive maintenance using Kafka and Java + Spring Boot.

## Problem Statement

Machines in factories or plants can fail unexpectedly, causing downtime and financial loss. Predictive maintenance helps identify issues before they become critical, optimizing operational efficiency. The system replays events to analyze past states or diagnose issues, providing full traceability.

## Key Features

- **Device Simulation**: Simulate multiple machines sending sensor data (temperature, vibration, load) to Kafka
- **Digital Twin Service**: Maintains stateful representation of each machine, updates metrics in real-time, and detects anomalies
- **Alert & Analytics Service**: Logs anomalies, triggers maintenance alerts, and computes analytics (average load, downtime, abnormal patterns)
- **Event Replay**: System can rebuild machine states from Kafka logs, allowing historical analysis or debugging
- **Scalability**: Can simulate hundreds of machines concurrently using Kafka partitions
- **Real-time Communication**: Both REST APIs and WebSocket endpoints for real-time monitoring and historical data access

## Technology Stack

- **Backend Framework**: Java 21 + Spring Boot 3.x
- **Event Streaming**: Apache Kafka (Confluent KRaft mode)
- **Database**: PostgreSQL
- **Containerization**: Docker with multi-stage builds
- **Monitoring**: Spring Boot Actuator

## Architecture Overview

```
[Device Simulator Service] ---> Kafka Topics ---> [Digital Twin Service] ---> [Alert/Analytics Service]
```

## Why This Project is Impressive

- **Unique & Rare**: Industrial digital twin + predictive maintenance is uncommon in CVs
- **Backend-Heavy**: Fully demonstrates Kafka expertise — event sourcing, replay, partitions, consumer groups, DLQs, exactly-once semantics
- **Real-World Relevance**: Solves a tangible problem seen in factories, energy plants, or production lines
- **Scalable & Observable**: Can simulate hundreds of machines, replay events, and compute analytics — great talking points for interviews

## Getting Started

1. Ensure Docker Desktop with WSL2 is installed and configured with at least 5GB memory
2. Clone the repository
3. Run `docker-compose up` to start all services
4. The system will simulate 15 different types of industrial machines
5. Monitor the system through the exposed endpoints

## Project Structure

- `device-simulator/` - Simulates 15 different industrial machine types
- `digital-twin/` - Maintains real-time state and detects anomalies
- `alert-analytics/` - Processes alerts and performs analytics
- `docker-compose.yml` - Orchestrates all services
- `docs/` - Documentation files