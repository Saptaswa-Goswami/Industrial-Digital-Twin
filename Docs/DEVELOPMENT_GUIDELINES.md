# Development Guidelines

## Overview

This document provides comprehensive guidelines for developing, testing, and maintaining the Industrial Equipment Digital Twin system. These guidelines ensure consistent code quality, maintainability, and adherence to best practices across all services.

## Project Structure

### Standard Service Structure
Each service should follow this standard directory structure:

```
service-name/
├── src/main/java/com/industrial/digitaltwin/servicename/
│   ├── ServiceNameApplication.java
│   ├── config/
│   │   ├── AppConfig.java
│   │   └── ServiceConfig.java
│   ├── model/
│   │   ├── EntityModels.java
│   │   └── DTOs.java
│   ├── service/
│   │   ├── ServiceImplementation.java
│   │   └── BusinessLogic.java
│   ├── repository/
│   │   └── RepositoryInterfaces.java
│   ├── controller/
│   │   └── RestControllers.java
│   ├── processor/
│   │   └── EventProcessors.java
│   └── util/
│       └── UtilityClasses.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   └── bootstrap.yml
├── src/test/java/
│   ├── unit/
│   ├── integration/
│   └── contract/
├── Dockerfile
├── pom.xml
└── README.md
```

## Coding Standards

### Java Standards
- **Java Version**: Java 21
- **Naming Conventions**:
  - Classes: PascalCase (e.g., `DigitalTwinService`)
  - Methods: camelCase (e.g., `detectAnomalies`)
  - Constants: UPPER_SNAKE_CASE (e.g., `DEFAULT_TIMEOUT`)
  - Variables: camelCase (e.g., `machineId`)

### Spring Boot Standards
- Use constructor injection over field injection
- Follow Spring's dependency injection best practices
- Use `@ConfigurationProperties` for configuration classes
- Use `@Validated` and `@Valid` for input validation
- Implement proper exception handling with `@ControllerAdvice`

### Code Organization
- Keep classes focused on a single responsibility
- Use interfaces for defining contracts
- Implement proper separation of concerns
- Follow the layered architecture (Controller → Service → Repository)

## Architecture Guidelines

### Service Design Principles
- **Single Responsibility**: Each class should have one reason to change
- **Open/Closed**: Open for extension, closed for modification
- **Dependency Inversion**: Depend on abstractions, not concretions
- **Interface Segregation**: Create specific interfaces rather than large general ones

### Event-Driven Architecture
- All service communication through Kafka
- No direct service-to-service calls
- Implement idempotent event processing
- Use event sourcing patterns where appropriate

### Future-Proofing Implementation
- Use Strategy Pattern for different algorithms
- Use Adapter Pattern for different data sources
- Implement configuration-driven behavior
- Design extensible data models

## Common Data Models for All Services

When implementing new services (like the Alert/Analytics service), ensure the following data models are consistent across all services to avoid deserialization issues like those experienced between the device simulator and digital twin services:

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

## Implementation Guidelines

1. Both device simulator and digital twin services now use the unified MachineStatus enum with all 8 values
2. Kafka serialization/deserialization is configured to not include type headers for compatibility
3. Status values are now consistent across services with no conversion needed
4. All services should use the same data models to ensure compatibility

## Data Models and Entities

### Entity Design
```java
@Entity
@Table(name = "alerts")
public class AlertEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "alert_id", unique = true, nullable = false)
    private String alertId;
    
    @Column(name = "machine_id", nullable = false)
    private String machineId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private AlertSeverity severity;
    
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
    
    @Column(name = "description", length = 1000)
    private String description;
    
    @Type(JsonType.class)
    @Column(name = "details", columnDefinition = "jsonb")
    private Map<String, Object> details;
    
    // Constructors, getters, setters
}
```

### DTO Design
```java
public class MachineTelemetryDTO {
    private String machineId;
    private Instant timestamp;
    private SensorDataDTO sensorData;
    private String schemaVersion;
    
    // Validation annotations
    @NotBlank
    private String machineId;
    
    @NotNull
    private Instant timestamp;
    
    @Valid
    private SensorDataDTO sensorData;
    
    // Constructors, getters, setters
}
```

## Configuration Management

### Application Properties
- Use `application.yml` for default configuration
- Use profile-specific files for environment-specific settings
- Externalize sensitive information using environment variables
- Use `@ConfigurationProperties` for complex configuration objects

### Configuration Properties Class
```java
@ConfigurationProperties(prefix = "app.digital-twin")
@Validated
@Data
@Component
public class DigitalTwinProperties {
    private Duration stateRetention = Duration.ofHours(24);
    private Duration snapshotInterval = Duration.ofMinutes(5);
    
    @Valid
    private AnomalyDetectionProperties anomalyDetection = new AnomalyDetectionProperties();
    
    @Valid
    private StateStoreProperties stateStore = new StateStoreProperties();
    
    @Data
    public static class AnomalyDetectionProperties {
        private double temperatureThreshold = 85.0;
        private double vibrationThreshold = 3.0;
        private double loadThreshold = 95.0;
        private double statisticalDeviationMultiplier = 2.5;
    }
    
    @Data
    public static class StateStoreProperties {
        private int maxEntries = 10000;
        private Duration evictionTimeout = Duration.ofHours(1);
    }
}
```

## Testing Strategy

### Unit Testing
- Achieve minimum 80% code coverage
- Test all business logic methods
- Use Mockito for dependency mocking
- Test edge cases and error conditions

### Integration Testing
- Test service-to-service communication
- Test database operations
- Test Kafka producer/consumer functionality
- Use Testcontainers for integration tests

### Test Example
```java
@SpringBootTest
@TestPropertySource(properties = {
    "kafka.bootstrap-servers=${testcontainers.kafka.bootstrap-servers}",
    "spring.datasource.url=${testcontainers.postgres.jdbc-url}"
})
class AnomalyDetectionServiceTest {
    
    @Autowired
    private AnomalyDetectionService anomalyDetectionService;
    
    @Test
    void shouldDetectTemperatureAnomaly() {
        // Given
        DigitalTwinState state = createTestState();
        MachineTelemetry telemetry = createHighTemperatureTelemetry();
        
        // When
        AnomalyEvent result = anomalyDetectionService.detectAnomalies(state, telemetry);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAnomalies())
            .hasSize(1)
            .extracting(Anomaly::getType)
            .contains(AnomalyType.TEMPERATURE_HIGH);
    }
}
```

## Security Guidelines

### Input Validation
- Validate all input parameters
- Use Bean Validation annotations
- Implement custom validation where needed
- Sanitize user inputs to prevent injection attacks

### Configuration Security
- Never hardcode sensitive information
- Use environment variables for secrets
- Implement secure configuration loading
- Regular security reviews of configuration

### API Security
- Implement proper authentication and authorization
- Use HTTPS in production
- Implement rate limiting
- Add proper logging for security events

## Performance Guidelines

### Memory Management
- Use appropriate data structures
- Implement proper object pooling where needed
- Monitor memory usage regularly
- Implement efficient caching strategies

### Database Performance
- Use proper indexing strategies
- Optimize queries with EXPLAIN ANALYZE
- Use connection pooling (HikariCP)
- Implement pagination for large datasets

### Kafka Performance
- Optimize partitioning strategies
- Use appropriate serialization formats
- Monitor consumer lag
- Implement proper batching

## Monitoring and Observability

### Logging Standards
- Use structured logging with JSON format
- Include correlation IDs for request tracing
- Log at appropriate levels (DEBUG, INFO, WARN, ERROR)
- Avoid logging sensitive information

### Metrics Collection
- Use Micrometer for metrics collection
- Implement custom business metrics
- Monitor service health and performance
- Set up alerts for critical metrics

### Health Checks
- Implement comprehensive health indicators
- Monitor external dependencies
- Provide meaningful health check responses
- Use readiness and liveness probes

## Error Handling

### Exception Handling
- Create custom exception classes for business logic
- Use `@ControllerAdvice` for global exception handling
- Provide meaningful error messages
- Log errors with appropriate context

### Error Response Format
```java
public class ErrorResponse {
    private String errorId;
    private String message;
    private String details;
    private Instant timestamp;
    private String path;
}
```

### Circuit Breaker Implementation
```java
@Component
public class ExternalServiceClient {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @CircuitBreaker(name = "external-service", fallbackMethod = "fallbackCall")
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public String callExternalService(String url) {
        return restTemplate.getForObject(url, String.class);
    }
    
    public String fallbackCall(String url, Exception ex) {
        log.warn("Fallback method called for URL: {}", url, ex);
        return "Service unavailable";
    }
}
```

## WebSocket Implementation Guidelines

### General Guidelines
- Use Spring WebSocket for consistent implementation across services
- Implement proper session management and connection lifecycle
- Handle connection errors and implement reconnection strategies
- Validate incoming messages from WebSocket clients
- Implement rate limiting to prevent message flooding
- Use JSON for message serialization
- Include proper error handling within WebSocket handlers
- Implement heartbeat mechanisms to detect broken connections

### Digital Twin Service WebSocket
- Endpoint: `/ws/machine-updates`
- Purpose: Real-time machine state updates
- Message format: Include machine ID, timestamp, sensor data, and status
- Broadcasting: Push updates when machine states change

### Alert/Analytics Service WebSocket
- Endpoint: `/ws/alert-updates`
- Purpose: Real-time alert and analytics notifications
- Message format: Include alert type, severity, machine ID, and payload
- Broadcasting: Push new alerts and analytics updates

## Deployment Guidelines

### Docker Best Practices
- Use multi-stage builds
- Run containers as non-root users
- Implement health checks
- Use appropriate resource limits

### Configuration for Different Environments
- Use Spring profiles for environment-specific configuration
- Externalize environment-specific settings
- Implement configuration validation
- Use infrastructure as code

## Code Quality Standards

### Static Analysis
- Use SonarQube or similar tools
- Implement code quality gates
- Regular code reviews
- Maintain code coverage thresholds

### Documentation
- Document public APIs
- Include meaningful class and method comments
- Maintain README files for each service
- Document configuration properties

## Future Enhancement Guidelines

### Adding New Features
- Follow existing architectural patterns
- Maintain backward compatibility
- Update documentation
- Add appropriate tests

### Migration Strategy
- Use configuration-driven feature activation
- Implement gradual rollout capabilities
- Maintain fallback mechanisms
- Monitor feature adoption

## Git Workflow

### Branching Strategy
- Use feature branches for new functionality
- Follow GitFlow or GitHub Flow
- Implement pull request reviews
- Maintain clean commit history

### Commit Messages
- Use conventional commits format
- Write meaningful commit messages
- Reference issue numbers when applicable
- Keep commits focused and atomic

## Continuous Integration

### Build Process
- Implement automated builds
- Run tests at multiple levels
- Perform static analysis
- Generate code coverage reports

### Deployment Pipeline
- Implement automated testing
- Use staging environments
- Implement blue-green deployments
- Monitor deployment success

These guidelines ensure consistent, maintainable, and production-ready code across all services in the Industrial Equipment Digital Twin system.