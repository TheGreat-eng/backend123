package com.example.iotserver.service;

import com.example.iotserver.dto.SensorDataDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Send sensor data to specific farm subscribers
     */
    public void sendSensorData(Long farmId, SensorDataDTO data) {
        String destination = "/topic/farm/" + farmId + "/sensor-data";
        messagingTemplate.convertAndSend(destination, data);
        log.debug("Sent sensor data to {}", destination);
    }

    /**
     * Send alert to farm subscribers
     */
    public void sendAlert(Long farmId, Map<String, Object> alert) {
        String destination = "/topic/farm/" + farmId + "/alerts";
        messagingTemplate.convertAndSend(destination, alert);
        log.info("Sent alert to {}", destination);
    }

    /**
     * Send device status update
     */
    public void sendDeviceStatus(Long farmId, String deviceId, String status) {
        String destination = "/topic/farm/" + farmId + "/device-status";
        Map<String, String> message = Map.of(
                "deviceId", deviceId,
                "status", status,
                "timestamp", String.valueOf(System.currentTimeMillis()));
        messagingTemplate.convertAndSend(destination, message);
        log.debug("Sent device status update to {}", destination);
    }

    public void sendDeviceStatus(Long farmId, Map<String, Object> statusPayload) {
        String destination = "/topic/farm/" + farmId + "/device-status";
        messagingTemplate.convertAndSend(destination, statusPayload);
        log.debug("Sent device status update to {}: {}", destination, statusPayload);
    }

    /**
     * Broadcast system notification
     */
    public void broadcastNotification(String message) {
        messagingTemplate.convertAndSend("/topic/notifications", Map.of(
                "message", message,
                "timestamp", System.currentTimeMillis()));
    }
}
