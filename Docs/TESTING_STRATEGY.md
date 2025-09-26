# Testing Strategy

## Overview

This document outlines the comprehensive testing strategy for the Industrial Equipment Digital Twin system. The strategy ensures high-quality, reliable, and maintainable code across all services while supporting the system's complex event-driven architecture.

## Testing Philosophy

### Quality Assurance Goals
- Achieve minimum 85% code coverage across all services
- Ensure reliable event processing and state management
- Validate system behavior under various load conditions
- Verify data integrity and consistency across services
- Test fault tolerance and recovery mechanisms

### Testing Principles
- Test early and often in the development cycle
- Use the testing pyramid approach (unit > integration > end-to-end)
- Implement automated testing at all levels
- Maintain fast feedback loops
- Ensure tests are reliable, maintainable, and meaningful

## Testing Levels

### 1. Unit Testing

#### Scope
- Individual methods and classes
- Business logic validation
- Algorithm correctness
- Data model validation

#### Tools and Frameworks
- **JUnit 5**: Primary testing framework
- **Mockito**: Dependency mocking
- **AssertJ**: Fluent assertions
- **TestNG**: Alternative testing framework (if needed)

#### Unit Test Examples

**Anomaly Detection Unit Test:**
```java
@ExtendWith(MockitoExtension.class)
class AnomalyDetectionServiceTest {
    
    @Mock
    private DigitalTwinStateRepository stateRepository;
    
    @InjectMocks
    private AnomalyDetectionService anomalyDetectionService;
    
    @Test
    void shouldDetectHighTemperatureAnomaly() {
        // Given
        DigitalTwinState state = DigitalTwinState.builder()
            .machineId("PUMP_001")
            .currentSensorData(SensorData.builder()
                .temperature(90.0) // Above threshold
                .vibration(1.5)
                .load(80.0)
                .build())
            .build();
        
        MachineTelemetry telemetry = MachineTelemetry.builder()
            .machineId("PUMP_001")
            .sensorData(SensorData.builder()
                .temperature(95.0) // Critical temperature
                .vibration(1.6)
                .load(82.0)
                .build())
            .build();
        
        // When
        AnomalyEvent result = anomalyDetectionService.detectAnomalies(state, telemetry);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMachineId()).isEqualTo("PUMP_001");
        assertThat(result.getAnomalies()).hasSize(1);
        assertThat(result.getAnomalies().get(0).getType()).isEqualTo(AnomalyType.TEMPERATURE_HIGH);
        assertThat(result.getAnomalies().get(0).getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
    }
    
    @Test
    void shouldNotDetectAnomalyWhenWithinThreshold() {
        // Given
        DigitalTwinState state = createNormalState();
        MachineTelemetry telemetry = createNormalTelemetry();
        
        // When
        AnomalyEvent result = anomalyDetectionService.detectAnomalies(state, telemetry);
        
        // Then
        assertThat(result).isNull();
    }
}
```

**Configuration Properties Unit Test:**
```java
@TestPropertySource(properties = {
    "app.digital-twin.anomaly-detection.temperature-threshold=80.0",
    "app.digital-twin.state-store.max-entries=5000"
})
class DigitalTwinPropertiesTest {
    
    @Test
    void shouldLoadConfigurationProperties() {
        // Given
        ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class)
            .withPropertyValues(
                "app.digital-twin.anomaly-detection.temperature-threshold=80.0",
                "app.digital-twin.state-store.max-entries=5000"
            );
        
        // When & Then
        contextRunner.run(context -> {
            DigitalTwinProperties properties = context.getBean(DigitalTwinProperties.class);
            assertThat(properties.getAnomalyDetection().getTemperatureThreshold()).isEqualTo(80.0);
            assertThat(properties.getStateStore().getMaxEntries()).isEqualTo(5000);
        });
    }
}
```

#### Unit Testing Best Practices
- Test all business logic methods
- Test edge cases and boundary conditions
- Test error handling and exception scenarios
- Mock external dependencies
- Keep tests fast and focused
- Use meaningful test method names

### 2. Integration Testing

#### Scope
- Service-to-service communication
- Database operations
- Kafka producer/consumer functionality
- External API integrations
- Configuration loading

#### Tools and Frameworks
- **Spring Boot Test**: Integration testing support
- **Testcontainers**: Docker-based integration tests
- **Embedded Kafka**: Local Kafka for testing
- **WireMock**: HTTP client testing
- **TestNG**: Additional testing framework

#### Integration Test Examples

**Kafka Integration Test:**
```java
@SpringBootTest
@TestPropertySource(properties = {"kafka.bootstrap-servers=${testcontainers.kafka.bootstrap-servers}"})
@Testcontainers
class KafkaIntegrationTest {
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"))
            .withEmbeddedZookeeper(); // Note: For testing, we use embedded zookeeper
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Autowired
    private ConsumerService consumerService;
    
    @Test
    void shouldProduceAndConsumeMessages() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        String testMachineId = "TEST_MACHINE_001";
        MachineTelemetry testTelemetry = createTestTelemetry(testMachineId);
        
        // Mock consumer behavior
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(consumerService).processTelemetry(any(MachineTelemetry.class));
        
        // When
        kafkaTemplate.send("machine-telemetry", testMachineId, testTelemetry);
        
        // Then
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        verify(consumerService, times(1)).processTelemetry(testTelemetry);
    }
}
```

**Database Integration Test:**
```java
@SpringBootTest
@TestPropertySource(properties = {"spring.datasource.url=${testcontainers.postgres.jdbc-url}"})
@Testcontainers
class AlertRepositoryIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @Autowired
    private AlertRepository alertRepository;
    
    @Test
    void shouldSaveAndRetrieveAlert() {
        // Given
        AlertEntity alert = AlertEntity.builder()
            .alertId("ALERT_001")
            .machineId("MACHINE_001")
            .severity(AlertSeverity.CRITICAL)
            .timestamp(Instant.now())
            .description("Test alert")
            .details(Map.of("test", "value"))
            .build();
        
        // When
        AlertEntity savedAlert = alertRepository.save(alert);
        Optional<AlertEntity> retrievedAlert = alertRepository.findById(savedAlert.getId());
        
        // Then
        assertThat(retrievedAlert).isPresent();
        assertThat(retrievedAlert.get().getAlertId()).isEqualTo("ALERT_001");
        assertThat(retrievedAlert.get().getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
    }
    
    @Test
    void shouldFindByMachineId() {
        // Given
        String machineId = "MACHINE_001";
        createTestAlerts(machineId);
        
        // When
        List<AlertEntity> alerts = alertRepository.findByMachineId(machineId);
        
        // Then
        assertThat(alerts).hasSize(3);
        assertThat(alerts).allMatch(alert -> alert.getMachineId().equals(machineId));
    }
}
```

#### Integration Testing Best Practices
- Use Testcontainers for realistic test environments
- Test actual database operations
- Verify Kafka message processing
- Test configuration loading with real values
- Test service startup and initialization

### 3. Contract Testing

#### Scope
- API contract validation
- Consumer-driven contracts
- Event schema validation

#### Tools and Frameworks
- **Spring Cloud Contract**: Consumer-driven contract testing
- **Pact**: Alternative contract testing framework

#### Contract Test Example
```java
@ExtendWith(SpringExtension.class)
@AutoConfigureJsonTesters
class AlertControllerContractTest {
    
    @Autowired
    private JacksonTester<AlertRequest> jsonAlertRequest;
    
    @Test
    void shouldSerializeAlertRequest() throws Exception {
        // Given
        AlertRequest request = AlertRequest.builder()
            .machineId("MACHINE_001")
            .severity("CRITICAL")
            .description("Test alert")
            .build();
        
        // When
        JsonContent<AlertRequest> jsonContent = jsonAlertRequest.write(request);
        
        // Then
        jsonContent.assertThat("$.machineId").isEqualTo("MACHINE_001");
        jsonContent.assertThat("$.severity").isEqualTo("CRITICAL");
        jsonContent.assertThat("$.description").isEqualTo("Test alert");
    }
}
```

### 4. Performance Testing

#### Scope
- Load testing under various conditions
- Stress testing for maximum capacity
- Endurance testing for long-running operations
- Database performance under load

#### Tools and Frameworks
- **JMeter**: Load testing tool
- **Gatling**: Alternative load testing framework
- **Spring's @Timed**: Performance metrics

#### Performance Test Example
```java
@SpringBootTest
class PerformanceTest {
    
    @Test
    @Timed
    void shouldHandleHighThroughput() {
        // Given
        int messageCount = 1000;
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        // When
        for (int i = 0; i < messageCount; i++) {
            final int index = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                MachineTelemetry telemetry = createTelemetry("MACHINE_" + (index % 100));
                // Send telemetry to Kafka
            });
            futures.add(future);
        }
        
        // Wait for all futures to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // Then
        // Verify all messages were processed
    }
}
```

### 5. End-to-End Testing

#### Scope
- Complete system workflow validation
- Cross-service functionality
- Data flow validation
- Alert generation and notification

#### End-to-End Test Example
```java
@SpringBootTest
@TestPropertySource(properties = {
    "kafka.bootstrap-servers=${testcontainers.kafka.bootstrap-servers}",
    "spring.datasource.url=${testcontainers.postgres.jdbc-url}"
})
@Testcontainers
class EndToEndTest {
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"))
            .withEmbeddedZookeeper();
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @Autowired
    private DeviceSimulationService simulationService;
    
    @Autowired
    private DigitalTwinService digitalTwinService;
    
    @Autowired
    private AlertAnalyticsService alertAnalyticsService;
    
    @Autowired
    private AlertRepository alertRepository;
    
    @Test
    void shouldProcessCompleteWorkflow() throws InterruptedException {
        // Given
        CountDownLatch alertLatch = new CountDownLatch(1);
        String testMachineId = "E2E_TEST_MACHINE_001";
        
        // Monitor for alert creation
        doAnswer(invocation -> {
            alertLatch.countDown();
            return null;
        }).when(alertAnalyticsService).processAnomalyEvent(any(AnomalyEvent.class));
        
        // When: Simulate high temperature anomaly
        MachineTelemetry anomalyTelemetry = createHighTemperatureTelemetry(testMachineId);
        simulationService.publishTelemetry(anomalyTelemetry);
        
        // Then: Verify complete workflow
        assertThat(alertLatch.await(30, TimeUnit.SECONDS)).isTrue();
        
        // Verify alert was created in database
        List<AlertEntity> alerts = alertRepository.findByMachineId(testMachineId);
        assertThat(alerts).isNotEmpty();
        assertThat(alerts).anyMatch(alert -> alert.getSeverity() == AlertSeverity.CRITICAL);
    }
}
```

## Test Organization

### Test Directory Structure
```
src/test/java/
├── unit/
│   ├── service/
│   ├── repository/
│   └── util/
├── integration/
│   ├── service/
│   ├── controller/
│   └── kafka/
├── contract/
│   └── api/
└── performance/
    └── load/
```

### Test Naming Conventions
- Use descriptive test method names
- Follow Given-When-Then pattern in names
- Group related tests in test classes
- Use nested test classes for complex scenarios

## Test Coverage Strategy

### Coverage Goals
- **Minimum 85%** overall code coverage
- **90%** for business logic
- **80%** for configuration classes
- **100%** for critical path validation

### Coverage Analysis Tools
- **JaCoCo**: Code coverage analysis
- **SonarQube**: Quality gate enforcement
- **Istanbul**: Alternative coverage tool

## Testing Automation

### CI/CD Integration
- Run unit tests on every commit
- Run integration tests on pull requests
- Execute performance tests in staging environment
- Generate test reports and coverage metrics

### Test Execution Strategy
```yaml
# Example GitHub Actions workflow
name: Test Suite
on: [push, pull_request]
jobs:
 unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 21
        uses: actions/setup-java@v2
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Run unit tests
        run: ./mvnw test
      - name: Publish test results
        uses: EnricoMi/publish-unit-test-result-action@v1
        with:
          files: target/surefire-reports/*.xml
  
  integration-tests:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: password
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 21
        uses: actions/setup-java@v2
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Run integration tests
        run: ./mvnw verify -Pintegration
```

## Test Data Management

### Test Data Strategies
- Use factory methods for creating test data
- Implement test data builders for complex objects
- Use embedded databases for isolation
- Implement data cleanup strategies

### Test Data Example
```java
@Component
public class TestDataFactory {
    
    public MachineTelemetry createNormalTelemetry(String machineId) {
        return MachineTelemetry.builder()
            .machineId(machineId)
            .timestamp(Instant.now())
            .sensorData(SensorData.builder()
                .temperature(70.0)
                .vibration(1.2)
                .load(75.0)
                .pressure(8.5)
                .rpm(1500)
                .build())
            .build();
    }
    
    public MachineTelemetry createHighTemperatureTelemetry(String machineId) {
        return MachineTelemetry.builder()
            .machineId(machineId)
            .timestamp(Instant.now())
            .sensorData(SensorData.builder()
                .temperature(95.0) // Above threshold
                .vibration(1.2)
                .load(75.0)
                .pressure(8.5)
                .rpm(1500)
                .build())
            .build();
    }
    
    public DigitalTwinState createNormalState(String machineId) {
        return DigitalTwinState.builder()
            .machineId(machineId)
            .lastUpdated(Instant.now())
            .currentSensorData(SensorData.builder()
                .temperature(70.0)
                .vibration(1.2)
                .load(75.0)
                .build())
            .status(MachineStatus.NORMAL)
            .operationalHours(1000)
            .efficiencyRating(95.0)
            .build();
    }
}
```

## Test Maintenance

### Test Refactoring
- Regular test code reviews
- Refactor tests when business logic changes
- Update tests when interfaces change
- Maintain test documentation

### Test Quality Assurance
- Eliminate flaky tests
- Ensure test independence
- Maintain fast execution times
- Regular test cleanup and optimization

## Monitoring Test Results

### Test Metrics
- Test execution time
- Test success/failure rates
- Code coverage metrics
- Performance benchmarks

### Test Reporting
- Generate detailed test reports
- Track test trends over time
- Identify test maintenance needs
- Share results with stakeholders

This comprehensive testing strategy ensures the Industrial Equipment Digital Twin system maintains high quality, reliability, and maintainability throughout its lifecycle.