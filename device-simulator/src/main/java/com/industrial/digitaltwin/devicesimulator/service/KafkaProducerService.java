package com.industrial.digitaltwin.devicesimulator.service;

import com.industrial.digitaltwin.devicesimulator.model.MachineTelemetry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, MachineTelemetry> kafkaTemplate;

    @Value("${app.kafka.telemetry-topic: machine-telemetry}")
    private String telemetryTopic;

    public CompletableFuture<SendResult<String, MachineTelemetry>> sendTelemetry(MachineTelemetry telemetry) {
        log.debug("Sending telemetry to Kafka: {}", telemetry.getMachineId());
        
        CompletableFuture<SendResult<String, MachineTelemetry>> future = 
            kafkaTemplate.send(telemetryTopic, telemetry.getMachineId(), telemetry);
        
        future.thenAccept(result -> 
            log.debug("Successfully sent telemetry for machine {}: offset={}, partition={}", 
                telemetry.getMachineId(), 
                result.getRecordMetadata().offset(), 
                result.getRecordMetadata().partition())
        ).exceptionally(ex -> {
            log.error("Failed to send telemetry for machine {}: {}", 
                telemetry.getMachineId(), ex.getMessage());
            return null;
        });
        
        return future;
    }
}