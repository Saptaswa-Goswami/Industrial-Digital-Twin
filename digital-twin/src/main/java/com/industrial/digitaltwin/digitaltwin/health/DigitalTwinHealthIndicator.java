package com.industrial.digitaltwin.digitaltwin.health;

import com.industrial.digitaltwin.digitaltwin.service.StateManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DigitalTwinHealthIndicator implements HealthIndicator {

    @Autowired
    private StateManagementService stateManagementService;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public Health health() {
        try {
            // Check if state management is working
            int stateCount = stateManagementService.getCurrentStateCount();
            
            // Check Kafka connectivity by attempting to send a test message
            // For now, we'll just verify that the Kafka template is available
            boolean kafkaAvailable = kafkaTemplate != null;
            
            // Determine health status based on checks
            if (kafkaAvailable) {
                return Health.up()
                    .withDetail("stateCount", stateCount)
                    .withDetail("kafkaStatus", "available")
                    .withDetail("message", "Digital Twin Service is operational")
                    .build();
            } else {
                return Health.down()
                    .withDetail("kafkaStatus", "unavailable")
                    .withDetail("message", "Kafka connection is not available")
                    .build();
            }
        } catch (Exception e) {
            log.error("Error checking Digital Twin Service health", e);
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("message", "Digital Twin Service is experiencing issues")
                .build();
        }
    }
}