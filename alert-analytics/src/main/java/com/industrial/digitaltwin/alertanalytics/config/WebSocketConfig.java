package com.industrial.digitaltwin.alertanalytics.config;

import com.industrial.digitaltwin.alertanalytics.websocket.AlertAnalyticsWebSocketHandler;
import com.industrial.digitaltwin.alertanalytics.websocket.ReplayWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private AlertAnalyticsWebSocketHandler alertAnalyticsWebSocketHandler;
    
    @Autowired
    private ReplayWebSocketHandler replayWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(alertAnalyticsWebSocketHandler, "/ws/alert-updates")
                .setAllowedOrigins("*");
                
        registry.addHandler(replayWebSocketHandler, "/ws/replay-updates")
                .setAllowedOrigins("*");
    }
}