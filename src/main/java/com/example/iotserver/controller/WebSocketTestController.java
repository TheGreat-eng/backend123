package com.example.iotserver.controller;

import com.example.iotserver.dto.SensorDataDTO;
import com.example.iotserver.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class WebSocketTestController {

    private final WebSocketService webSocketService;

    /**
     * Test gửi sensor data
     * GET /api/test/ws/sensor?farmId=1
     */
    @GetMapping("/ws/sensor")
    public Map<String, Object> testSensorData(@RequestParam Long farmId) {
        // Tạo test data giống format thật
        Map<String, Object> testData = new HashMap<>();
        testData.put("deviceId", "TEST-DEVICE-001");
        testData.put("temperature", 25.5);
        testData.put("humidity", 65.0);
        testData.put("soilMoisture", 45.0);
        testData.put("lightIntensity", 1200.0);
        testData.put("timestamp", LocalDateTime.now().toString());
        testData.put("farmId", farmId);

        // Gửi qua WebSocket
        // Convert testData map to SensorDataDTO
        SensorDataDTO sensorDataDTO = new SensorDataDTO();
        sensorDataDTO.setDeviceId((String) testData.get("deviceId"));
        sensorDataDTO.setTemperature((Double) testData.get("temperature"));
        sensorDataDTO.setHumidity((Double) testData.get("humidity"));
        sensorDataDTO.setSoilMoisture((Double) testData.get("soilMoisture"));
        sensorDataDTO.setLightIntensity((Double) testData.get("lightIntensity"));
        sensorDataDTO.setFarmId((Long) testData.get("farmId"));

        webSocketService.sendSensorData(farmId, sensorDataDTO);

        return Map.of(
                "status", "✅ Message sent successfully",
                "topic", "/topic/farm/" + farmId + "/sensor-data",
                "data", testData,
                "instruction", "Open HTML test file or browser console to receive this message");
    }

    /**
     * Test gửi alert
     * GET /api/test/ws/alert?farmId=1&type=HIGH_TEMP
     */
    @GetMapping("/ws/alert")
    public Map<String, Object> testAlert(
            @RequestParam Long farmId,
            @RequestParam(defaultValue = "TEST_ALERT") String type) {

        Map<String, Object> alert = new HashMap<>();
        alert.put("id", System.currentTimeMillis());
        alert.put("type", type);
        alert.put("message", "Test alert: Temperature exceeds threshold!");
        alert.put("severity", "WARNING");
        alert.put("farmId", farmId);
        alert.put("timestamp", LocalDateTime.now().toString());
        alert.put("deviceId", "TEST-DEVICE-001");

        webSocketService.sendAlert(farmId, alert);

        return Map.of(
                "status", "✅ Alert sent successfully",
                "topic", "/topic/farm/" + farmId + "/alerts",
                "alert", alert);
    }

    /**
     * Test gửi device status
     * GET /api/test/ws/device?farmId=1&deviceId=TEST-001&status=ONLINE
     */
    @GetMapping("/ws/device")
    public Map<String, Object> testDeviceStatus(
            @RequestParam Long farmId,
            @RequestParam String deviceId,
            @RequestParam(defaultValue = "ONLINE") String status) {

        webSocketService.sendDeviceStatus(farmId, deviceId, status);

        return Map.of(
                "status", "✅ Device status sent successfully",
                "topic", "/topic/farm/" + farmId + "/device-status",
                "deviceId", deviceId,
                "deviceStatus", status);
    }

    /**
     * Test broadcast notification
     * GET /api/test/ws/broadcast?message=Hello World
     */
    @GetMapping("/ws/broadcast")
    public Map<String, Object> testBroadcast(
            @RequestParam(defaultValue = "Test notification from backend") String message) {

        webSocketService.broadcastNotification(message);

        return Map.of(
                "status", "✅ Broadcast sent successfully",
                "topic", "/topic/notifications",
                "message", message,
                "timestamp", LocalDateTime.now().toString());
    }

    /**
     * Test gửi nhiều messages liên tục
     * GET /api/test/ws/stress?farmId=1&count=10
     */
    @GetMapping("/ws/stress")
    public Map<String, Object> stressTest(
            @RequestParam Long farmId,
            @RequestParam(defaultValue = "10") int count) throws InterruptedException {

        for (int i = 0; i < count; i++) {
            Map<String, Object> data = new HashMap<>();
            data.put("deviceId", "TEST-DEVICE-" + i);
            data.put("temperature", 20.0 + (Math.random() * 10));
            data.put("humidity", 50.0 + (Math.random() * 30));
            data.put("messageNumber", i + 1);
            data.put("timestamp", LocalDateTime.now().toString());

            // Convert data map to SensorDataDTO
            SensorDataDTO sensorDataDTO = new SensorDataDTO();
            sensorDataDTO.setDeviceId((String) data.get("deviceId"));
            sensorDataDTO.setTemperature((Double) data.get("temperature"));
            sensorDataDTO.setHumidity((Double) data.get("humidity"));
            // If you have soilMoisture and lightIntensity in your SensorDataDTO, set them
            // here if needed
            // sensorDataDTO.setSoilMoisture((Double) data.get("soilMoisture"));
            // sensorDataDTO.setLightIntensity((Double) data.get("lightIntensity"));
            sensorDataDTO.setFarmId(farmId);

            webSocketService.sendSensorData(farmId, sensorDataDTO);
            Thread.sleep(500); // 0.5s delay
        }

        return Map.of(
                "status", "✅ Sent " + count + " messages",
                "topic", "/topic/farm/" + farmId + "/sensor-data",
                "count", count);
    }

    /**
     * Health check endpoint
     * GET /api/test/ws/health
     */
    @GetMapping("/ws/health")
    public Map<String, Object> healthCheck() {
        return Map.of(
                "status", "✅ WebSocket Test API is running",
                "endpoints", Map.of(
                        "sensor", "/api/test/ws/sensor?farmId=1",
                        "alert", "/api/test/ws/alert?farmId=1",
                        "device", "/api/test/ws/device?farmId=1&deviceId=TEST-001",
                        "broadcast", "/api/test/ws/broadcast?message=Hello",
                        "stress", "/api/test/ws/stress?farmId=1&count=10"),
                "timestamp", LocalDateTime.now().toString());
    }
}
