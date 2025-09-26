package com.industrial.digitaltwin.digitaltwin.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class MetricsService {

    @Autowired
    private MeterRegistry meterRegistry;
    
    @Autowired
    private StateManagementService stateManagementService;
    
    private final AtomicLong processedEvents = new AtomicLong(0);
    private final AtomicLong detectedAnomalies = new AtomicLong(0);
    private final AtomicLong stateUpdates = new AtomicLong(0);
    
    private Counter eventProcessingCounter;
    private Counter anomalyDetectionCounter;
    private Counter stateUpdateCounter;

    @PostConstruct
    public void initializeMetrics() {
        log.info("Initializing metrics for Digital Twin Service");
        
        // Initialize counters
        eventProcessingCounter = Counter.builder("digital_twin.events.processed")
                .description("Number of telemetry events processed")
                .register(meterRegistry);
                
        anomalyDetectionCounter = Counter.builder("digital_twin.anomalies.detected")
                .description("Number of anomalies detected")
                .register(meterRegistry);
                
        stateUpdateCounter = Counter.builder("digital_twin.state.updates")
                .description("Number of state updates published")
                .register(meterRegistry);
        
        // Register gauges for current state
        Gauge.builder("digital_twin.state.count", stateManagementService, service -> service.getCurrentStateCount())
                .description("Current number of tracked machine states")
                .register(meterRegistry);
                
        Gauge.builder("digital_twin.memory.usage", stateManagementService, service -> service.getMemoryUsage())
                .description("Estimated memory usage")
                .register(meterRegistry);
        
        log.info("Metrics initialized successfully");
    }
    
    public void incrementProcessedEvents() {
        processedEvents.incrementAndGet();
        eventProcessingCounter.increment();
    }
    
    public void incrementDetectedAnomalies() {
        detectedAnomalies.incrementAndGet();
        anomalyDetectionCounter.increment();
    }
    
    public void incrementStateUpdates() {
        stateUpdates.incrementAndGet();
        stateUpdateCounter.increment();
    }
    
    public long getProcessedEventsCount() {
        return processedEvents.get();
    }
    
    public long getDetectedAnomaliesCount() {
        return detectedAnomalies.get();
    }
    
    public long getStateUpdatesCount() {
        return stateUpdates.get();
    }
}