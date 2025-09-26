# Alert/Analytics Service

## Overview

The Alert/Analytics Service consumes anomaly events and state updates from Kafka, generates alerts for maintenance teams, performs analytics on machine performance, and provides reporting capabilities for predictive maintenance insights.

## Architecture

### Core Components
1. **Kafka Consumer** - Consumes anomaly events and state updates
2. **Alert Management Engine** - Processes and manages alerts
3. **Analytics Engine** - Computes performance metrics and trends
4. **Notification Service** - Sends alerts via various channels
5. **Reporting Service** - Generates maintenance reports and dashboards

### Project Structure
```
alert-analytics/
├── src/main/java/com/industrial/digitaltwin/alertanalytics/
│   ├── AlertAnalyticsApplication.java
│   ├── config/
│   │   ├── KafkaConfig.java
│   │   ├── NotificationConfig.java
│   │   ├── AnalyticsConfig.java
│   │   └── WebSocketConfig.java
│   ├── model/
│   │   ├── AlertEvent.java
│   │   ├── AnomalyRecord.java
│   │   ├── MaintenanceReport.java
│   │   ├── PerformanceMetrics.java
│   │   └── NotificationChannel.java
│   ├── service/
│   │   ├── AlertProcessingService.java
│   │   ├── AnalyticsService.java
│   │   ├── NotificationService.java
│   │   ├── KafkaConsumerService.java
│   │   ├── ReportGenerationService.java
│   │   └── WebSocketBroadcastService.java
│   ├── repository/
│   │   ├── AlertRepository.java
│   │   ├── AnomalyRepository.java
│   │   └── PerformanceMetricsRepository.java
│   ├── controller/
│   │   └── AlertAnalyticsController.java
│   ├── websocket/
│   │   └── AlertAnalyticsWebSocketHandler.java
│   └── scheduler/
│   │   └── AnalyticsScheduler.java
├── src/main/resources/
│   ├── application.yml
│   └── application-dev.yml
└── Dockerfile
```

## Data Model Consistency Guidelines

When implementing the Alert/Analytics Service, follow these guidelines to ensure compatibility with other services and avoid deserialization issues like those experienced with the device simulator and digital twin services:

### 1. Use Unified MachineStatus Enum
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

### 2. Use Unified MachineTelemetry Model
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

### 3. Use Unified SensorData Model
```java
public class SensorData {
    private double temperature;
    private double vibration;
    private double load;
    private double pressure;
    private double rpm;
}
```

## Data Models

### AlertEvent.java
```java
public class AlertEvent {
    private String alertId;
    private String machineId;
    private AlertSeverity severity; // CRITICAL, WARNING, INFO
    private AlertType type; // OVERHEATING, EXCESSIVE_VIBRATION, etc.
    private Instant timestamp;
    private String description;
    private Map<String, Object> details;
    private AlertStatus status; // NEW, ACKNOWLEDGED, RESOLVED
    private String assignedTo;
    private Instant resolutionTime;
}

public enum AlertSeverity {
    CRITICAL(1), WARNING(2), INFO(3);
    
    private final int priority;
    AlertSeverity(int priority) { this.priority = priority; }
}
```

### PerformanceMetrics.java
```java
public class PerformanceMetrics {
    private String machineId;
    private Instant calculationTime;
    private double averageLoad;
    private double averageTemperature;
    private double averageVibration;
    private double uptimePercentage;
    private int anomalyCount;
    private double efficiencyRating;
    private Duration averageDowntime;
    private List<Trend> trends; // Historical trend data
}
```

## Kafka Integration

### Topics Consumed
- `anomaly-events` - Anomaly detections from digital twin service
- `machine-state-updates` - State changes for analytics

### Topics Produced
- `alerts` - Generated alerts for downstream consumption
- `analytics-reports` - Processed analytics data

### Consumer Configuration
- Multiple consumer groups for different event types
- Manual offset management for reliable processing
- Error handling with dead letter queue

### Proper Deserialization Configuration

To avoid deserialization issues like those experienced between the device simulator and digital twin services, configure Kafka consumers properly in the Alert/Analytics service:


### Consumer Configuration
```java
// Use ErrorHandlingDeserializer to handle deserialization errors gracefully
props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.industrial.digitaltwin.devicesimulator.model,com.industrial.digitaltwin.digitaltwin.model,com.industrial.digitaltwin.alertanalytics.model,java.lang,java.util");

// Deserialize to Map first for maximum compatibility
props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "java.util.Map");
```

### Producer Configuration
```java
// Don't add type information to avoid classpath issues
configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
```

## Alert Management System

### Alert Processing Pipeline
1. **Ingestion**: Receive anomaly events from Kafka
2. **Deduplication**: Filter duplicate alerts within time windows
3. **Correlation**: Group related anomalies from same machine
4. **Prioritization**: Assign severity based on anomaly type and impact
5. **Assignment**: Route alerts to appropriate maintenance teams
6. **Notification**: Send alerts via configured channels

### Alert Deduplication
- Time-based deduplication (prevent alert storms)
- Pattern-based correlation (group related issues)
- State-based filtering (avoid alerts for already-reported issues)

## Analytics Implementation

### Real-time Analytics
- Moving averages for key metrics
- Trend analysis for degradation detection
- Anomaly frequency tracking
- Machine efficiency calculations

### Batch Analytics
- Daily/weekly performance reports
- Maintenance prediction models
- Historical trend analysis
- Comparative analysis across machine types

### Analytics Service
```java
@Service
public class AnalyticsService {
    
    public PerformanceMetrics calculatePerformanceMetrics(String machineId, List<SensorData> historicalData) {
        // Calculate various performance indicators
        double averageLoad = calculateAverage(historicalData, SensorData::getLoad);
        double averageTemperature = calculateAverage(historicalData, SensorData::getTemperature);
        double averageVibration = calculateAverage(historicalData, SensorData::getVibration);
        
        // Calculate uptime based on operational status
        double uptimePercentage = calculateUptimePercentage(historicalData);
        
        // Count anomalies in the period
        int anomalyCount = countAnomalies(historicalData);
        
        // Calculate efficiency rating
        double efficiencyRating = calculateEfficiencyRating(averageLoad, uptimePercentage, anomalyCount);
        
        return PerformanceMetrics.builder()
            .machineId(machineId)
            .calculationTime(Instant.now())
            .averageLoad(averageLoad)
            .averageTemperature(averageTemperature)
            .averageVibration(averageVibration)
            .uptimePercentage(uptimePercentage)
            .anomalyCount(anomalyCount)
            .efficiencyRating(efficiencyRating)
            .build();
    }
}
```

## Notification System

### Multi-Channel Notifications
- **Email**: For non-critical alerts and reports
- **SMS**: For critical alerts requiring immediate attention
- **Slack**: For team notifications (future enhancement)
- **Webhook**: For integration with maintenance systems

### Notification Configuration
```yaml
app:
  notification:
    channels:
      email:
        enabled: true
        from: alerts@digitaltwin.com
        smtp-host: smtp.company.com
      sms:
        enabled: true
        provider: twilio
        account-sid: ${TWILIO_ACCOUNT_SID}
        auth-token: ${TWILIO_AUTH_TOKEN}
      webhook:
        enabled: true
        endpoints:
          - url: https://maintenance-system.com/api/alerts
            headers:
              Authorization: "Bearer ${MAINTENANCE_API_KEY}"
```

## Configuration Properties

### application.yml
```yaml
app:
  alert-analytics:
    alert-processing:
      deduplication-window: 5m # 5 minutes for duplicate filtering
      correlation-window: 10m   # 10 minutes for related alert grouping
      critical-threshold: 2     # Max time before escalation
    analytics:
      calculation-interval: 1m  # Calculate metrics every minute
      reporting-interval: 1h    # Generate reports every hour
      retention-period: 30d     # Keep analytics data for 30 days
    notification:
      critical-delay: 30s       # Delay before escalating critical alerts
      retry-attempts: 3         # Number of notification retry attempts

kafka:
  consumer:
    bootstrap-servers: kafka:9092
    group-id: alert-analytics-group
    key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
    auto-offset-reset: latest
    enable-auto-commit: false
```

## Database Schema (PostgreSQL)

```sql
-- Alerts table
CREATE TABLE alerts (
    id SERIAL PRIMARY KEY,
    alert_id VARCHAR(50) UNIQUE NOT NULL,
    machine_id VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    description TEXT,
    details JSONB,
    status VARCHAR(20) DEFAULT 'NEW',
    assigned_to VARCHAR(100),
    resolution_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Anomaly records
CREATE TABLE anomaly_records (
    id SERIAL PRIMARY KEY,
    machine_id VARCHAR(50) NOT NULL,
    anomaly_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    sensor_data JSONB,
    calculated_metrics JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Performance metrics
CREATE TABLE performance_metrics (
    id SERIAL PRIMARY KEY,
    machine_id VARCHAR(50) NOT NULL,
    calculation_time TIMESTAMP NOT NULL,
    average_load DECIMAL(5,2),
    average_temperature DECIMAL(5,2),
    average_vibration DECIMAL(5,2),
    uptime_percentage DECIMAL(5,2),
    anomaly_count INTEGER,
    efficiency_rating DECIMAL(5,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Scheduling and Reporting

### Analytics Scheduler
- Periodic performance metric calculations
- Regular report generation
- Maintenance prediction updates
- Data cleanup tasks

### Report Generation
- Daily operational summaries
- Weekly maintenance recommendations
- Monthly performance trends
- Custom ad-hoc reports

## WebSocket Real-time Updates

### WebSocket Configuration
- **Endpoint**: `/ws/alert-updates`
- **Message Format**: JSON with alert and analytics updates
- **Broadcast Strategy**: Push real-time alerts and analytics updates to connected clients
- **Session Management**: Track connected clients and handle connection lifecycle

### WebSocket Integration
- **Real-time Alerts**: Automatically push new alerts to connected WebSocket clients
- **Analytics Updates**: Push live analytics updates as they're calculated
- **Selective Broadcasting**: Option to broadcast to specific user groups or alert types
- **Connection Monitoring**: Track active connections and handle disconnections gracefully

### WebSocket Message Structure
```java
public class AlertAnalyticsUpdate {
    private String updateType; // ALERT, ANALYTICS, REPORT
    private Instant timestamp;
    private Object payload; // AlertEvent, PerformanceMetrics, or MaintenanceReport
    private String severity; // For alert updates
    private String machineId; // Associated machine ID
}
```

## Health Checks and Monitoring

### Actuator Endpoints
- Health: Kafka connectivity, database status, notification service status
- Metrics: Alert processing rates, analytics computation times, notification success rates
- Custom endpoints for alert status and report generation

## Error Handling and Resilience

### Circuit Breaker Pattern
- For notification service calls
- For database operations
- For external API integrations

### Retry Mechanisms
- Kafka consumer retries with exponential backoff
- Notification delivery retries
- Database operation retries

## Future-Proofing Implementation

### Notification Channel Strategy Pattern
```java
public interface NotificationChannel {
    void sendNotification(AlertEvent alert);
    boolean isAvailable();
    NotificationType getType();
}

@Component("email")
public class EmailNotificationChannel implements NotificationChannel {
    // Current implementation with email notifications
}

@Component("sms")
public class SmsNotificationChannel implements NotificationChannel {
    // Current implementation with SMS notifications
}

@Component("slack")
@ConditionalOnProperty(name = "app.notification.channels", havingValue = "slack")
public class SlackNotificationChannel implements NotificationChannel {
    // Future implementation with Slack notifications
}

@Component("microsoft-teams")
@ConditionalOnProperty(name = "app.notification.channels", havingValue = "msteams")
public class MicrosoftTeamsNotificationChannel implements NotificationChannel {
    // Future implementation with Microsoft Teams notifications
}
```

### Analytics Strategy Pattern
```java
public interface AnalyticsStrategy {
    PerformanceMetrics calculateMetrics(String machineId, List<SensorData> historicalData);
}

@Component("basic")
public class BasicAnalyticsStrategy implements AnalyticsStrategy {
    // Current implementation with basic metrics calculation
}

@Component("predictive")
@ConditionalOnProperty(name = "app.analytics.mode", havingValue = "predictive")
public class PredictiveAnalyticsStrategy implements AnalyticsStrategy {
    // Future implementation with ML-based predictions
}

@Component("trend-analysis")
@ConditionalOnProperty(name = "app.analytics.mode", havingValue = "trend-analysis")
public class TrendAnalysisStrategy implements AnalyticsStrategy {
    // Future implementation with trend analysis
}
```

### Configuration-Driven Behavior
```yaml
app:
  notification:
    mode: "email-sms"  # Can expand to include other channels
  analytics:
    mode: "basic"      # Can switch to "predictive" or "trend-analysis"
    basic:
      enabled: true
    predictive:
      enabled: false   # Enable when ready for ML-based analytics
    trend-analysis:
      enabled: false   # Enable for advanced trend analysis
```

## Implementation Phases

### Phase 1: Basic Alert Processing
- Create basic Spring Boot application
- Implement Kafka consumers for anomaly events
- Basic alert generation and storage
- Simple email notifications

### Phase 2: Analytics Implementation
- Implement performance metric calculations
- Add batch analytics processing
- Generate basic reports
- Add multiple notification channels

### Phase 3: Advanced Features
- Implement alert correlation and deduplication
- Add predictive analytics
- Optimize performance and memory usage
- Add comprehensive monitoring and alerting

## Quality Assurance

### Code Quality Targets
- 85%+ test coverage
- Performance benchmarks
- Integration testing
- Security scanning

### Performance Targets
- Process 1000+ events per second per instance
- Sub-100ms alert generation latency
- Memory usage under 600MB
- 99.9% notification delivery success rate

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
COPY --from=build /app/target/alert-analytics-*.jar app.jar

# Expose ports
EXPOSE 8080

# Optional: Add healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-Xms384m", "-Xmx600m", "-XX:+UseG1GC", "-jar", "/app.jar"]
```

## Success Criteria

1. **Functional**: Processes all anomaly events and generates appropriate alerts
2. **Reliable**: Maintains alert delivery with minimal failures
3. **Intelligent**: Provides meaningful analytics and maintenance insights
4. **Responsive**: Delivers critical alerts within required timeframes
5. **Comprehensive**: Generates useful reports for maintenance planning
6. **Observable**: Provides comprehensive operational metrics
7. **Future-Ready**: Architecture supports additional notification channels and ML-based analytics as per [FUTURE_PROOFING.md](FUTURE_PROOFING.md)
8. **Follows Standards**: Adheres to all coding standards from [DEVELOPMENT_GUIDELINES.md](DEVELOPMENT_GUIDELINES.md)
9. **Well-tested**: Achieves minimum 85% test coverage as per [TESTING_STRATEGY.md](TESTING_STRATEGY.md)