package com.industrial.digitaltwin.devicesimulator.health;

import com.industrial.digitaltwin.devicesimulator.service.MachineDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SimulationHealthIndicator implements HealthIndicator {

    private final MachineDataService machineDataService;

    @Override
    public Health health() {
        try {
            String dataSourceType = machineDataService.getActiveDataSourceType();
            
            if (dataSourceType != null && !dataSourceType.equals("unknown")) {
                return Health.up()
                    .withDetail("dataSourceType", dataSourceType)
                    .withDetail("message", "Simulation service is running")
                    .build();
            } else {
                return Health.down()
                    .withDetail("message", "No active data source found")
                    .build();
            }
        } catch (Exception e) {
            log.error("Error checking simulation health", e);
            return Health.down()
                .withDetail("message", "Error checking simulation health: " + e.getMessage())
                .build();
        }
    }
}