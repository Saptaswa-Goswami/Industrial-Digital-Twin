package com.industrial.digitaltwin.digitaltwin.service;

import com.industrial.digitaltwin.digitaltwin.model.DigitalTwinState;
import com.industrial.digitaltwin.digitaltwin.model.MachineStateUpdate;
import com.industrial.digitaltwin.digitaltwin.websocket.DigitalTwinWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WebSocketBroadcastService {

    @Autowired
    private DigitalTwinWebSocketHandler webSocketHandler;

    public void broadcastStateUpdate(DigitalTwinState state) {
        try {
            MachineStateUpdate update = MachineStateUpdate.builder()
                .machineId(state.getMachineId())
                .timestamp(state.getLastUpdated())
                .currentData(state.getCurrentSensorData())
                .status(state.getStatus())
                .isAnomalyDetected(state.getRecentAnomalies() != null && !state.getRecentAnomalies().isEmpty())
                .machineType("industrial-machine") // This would be determined based on machine configuration
                .build();
            
            webSocketHandler.broadcastMessage(update);
            log.debug("Broadcasted WebSocket update for machine: {}", state.getMachineId());
        } catch (Exception e) {
            log.error("Error broadcasting WebSocket update for machine: {}", state.getMachineId(), e);
        }
    }
}