package com.industrial.digitaltwin.digitaltwin.config;

import com.industrial.digitaltwin.digitaltwin.websocket.DigitalTwinWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private DigitalTwinWebSocketHandler digitalTwinWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(digitalTwinWebSocketHandler, "/ws/machine-updates")
                .setAllowedOrigins("*");
    }
}