// src/main/java/com/example/iotserver/service/MqttMessageHandler.java

package com.example.iotserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.iotserver.dto.SensorDataDTO;
import com.example.iotserver.entity.Device;
import com.example.iotserver.entity.Farm;
import com.example.iotserver.repository.DeviceRepository;
import com.example.iotserver.repository.FarmRepository;
import com.example.iotserver.service.PlantHealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class MqttMessageHandler {

    private final DeviceRepository deviceRepository;
    private final SensorDataService sensorDataService;
    private final WebSocketService webSocketService;
    private final ObjectMapper objectMapper;
    private final PlantHealthService plantHealthService;
    private final EmailService emailService; // <<<< Thêm vào
    private final FarmRepository farmRepository; // <<<< Thêm vào

    // <<<< THÊM CÁC HẰNG SỐ NÀY VÀO >>>>
    private static final double HIGH_TEMP_THRESHOLD = 38.0;
    private static final double LOW_SOIL_MOISTURE_THRESHOLD = 20.0;
    private static final double HIGH_HUMIDITY_THRESHOLD = 90.0;
    private static final int SENSOR_NOTIFICATION_COOLDOWN_HOURS = 4; // Gửi lại sau 4 giờ

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMessage(Message<?> message) {
        try {
            MessageHeaders headers = message.getHeaders();
            String topic = (String) headers.get("mqtt_receivedTopic");
            String payload = message.getPayload().toString();

            log.info("Received MQTT message - Topic: {}, Payload: {}", topic, payload);

            if (topic.startsWith("sensor/")) {
                handleSensorData(topic, payload);
            } else if (topic.startsWith("device/")) {
                handleDeviceStatus(topic, payload);
            }

        } catch (Exception e) {
            log.error("Error handling MQTT message: {}", e.getMessage(), e);
        }
    }

    @Transactional
    private void handleSensorData(String topic, String payload) {
        try {
            String deviceId = topic.split("/")[1];
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            SensorDataDTO sensorData = SensorDataDTO.fromMqttPayload(deviceId, data);

            // SỬA LỖI Ở ĐÂY: Tìm Device và Farm ID TRƯỚC KHI LƯU
            Device device = deviceRepository.findByDeviceIdWithFarmAndOwner(deviceId)
                    .orElse(null);

            if (device != null) {
                Long farmId = device.getFarm().getId();
                // Gán farmId vào DTO
                sensorData.setFarmId(farmId);

                // BÂY GIỜ MỚI LƯU DỮ LIỆU VÀO INFLUXDB (với farmId chính xác)
                sensorDataService.saveSensorData(sensorData);

                // Cập nhật trạng thái và lastSeen cho device
                device.setLastSeen(LocalDateTime.now());
                device.setStatus(Device.DeviceStatus.ONLINE);
                deviceRepository.save(device);

                // Gửi thông báo qua WebSocket
                webSocketService.sendSensorData(farmId, sensorData);

                // Kích hoạt phân tích sức khỏe cây trồng
                try {
                    log.info("Kích hoạt phân tích sức khỏe cho farmId: {}", farmId);
                    plantHealthService.analyzeHealth(farmId);
                } catch (Exception e) {
                    log.error("Lỗi khi kích hoạt phân tích sức khỏe cây trồng cho farmId {}: {}", farmId,
                            e.getMessage(), e);
                }

                // Kích hoạt kiểm tra cảnh báo tức thời
                checkSensorDataAnomaliesAndNotify(device, sensorData);
            }

            log.info("Processed sensor data from device: {}", deviceId);

        } catch (Exception e) {
            log.error("Error processing sensor data: {}", e.getMessage(), e);
        }
    }

    private void handleDeviceStatus(String topic, String payload) {
        try {
            String deviceId = topic.split("/")[1];
            Map<String, Object> status = objectMapper.readValue(payload, Map.class);
            deviceRepository.findByDeviceId(deviceId).ifPresent(device -> {
                String statusStr = status.get("status").toString();
                device.setStatus(Device.DeviceStatus.valueOf(statusStr.toUpperCase()));
                device.setLastSeen(LocalDateTime.now());
                deviceRepository.save(device);
                log.info("Updated device status: {} - {}", deviceId, statusStr);
            });
        } catch (Exception e) {
            log.error("Error processing device status: {}", e.getMessage(), e);
        }
    }

    // <<<<<<<<<<<<<<<< PHẦN BỊ THIẾU MÀ BẠN CẦN THÊM VÀO >>>>>>>>>>>>>>>>>>

    /**
     * Kiểm tra các giá trị bất thường từ dữ liệu cảm biến và gửi email thông báo.
     * Được đánh dấu @Transactional để đảm bảo việc cập nhật farm được thực hiện an
     * toàn.
     */
    @Transactional
    public void checkSensorDataAnomaliesAndNotify(Device device, SensorDataDTO data) {
        Farm farm = device.getFarm();
        String ownerEmail = farm.getOwner().getEmail();
        if (ownerEmail == null || ownerEmail.isEmpty()) {
            return; // Không có email để gửi
        }

        // 1. Kiểm tra nhiệt độ cao
        if (data.getTemperature() != null && data.getTemperature() > HIGH_TEMP_THRESHOLD) {
            if (canSendSensorNotification(farm.getLastHighTempSensorWarningAt())) {
                String subject = String.format("[SmartFarm Cảnh Báo] Nhiệt độ cao tại %s", farm.getName());
                String text = createSensorEmailText(farm, device, "Nhiệt độ", data.getTemperature(), "°C",
                        "cao bất thường", "Hãy kiểm tra hệ thống làm mát hoặc lưới che nắng.");
                emailService.sendSimpleMessage(ownerEmail, subject, text);
                farm.setLastHighTempSensorWarningAt(LocalDateTime.now());
                farmRepository.save(farm);
                log.info("Đã gửi email cảnh báo nhiệt độ cao (tức thời) cho farm {}", farm.getId());
            }
        }

        // 2. Kiểm tra độ ẩm đất thấp
        if (data.getSoilMoisture() != null && data.getSoilMoisture() < LOW_SOIL_MOISTURE_THRESHOLD) {
            if (canSendSensorNotification(farm.getLastLowSoilMoistureWarningAt())) {
                String subject = String.format("[SmartFarm Cảnh Báo] Độ ẩm đất thấp tại %s", farm.getName());
                String text = createSensorEmailText(farm, device, "Độ ẩm đất", data.getSoilMoisture(), "%",
                        "thấp đến mức báo động", "Hãy kiểm tra hệ thống tưới nước ngay lập tức.");
                emailService.sendSimpleMessage(ownerEmail, subject, text);
                farm.setLastLowSoilMoistureWarningAt(LocalDateTime.now());
                farmRepository.save(farm);
                log.info("Đã gửi email cảnh báo độ ẩm đất thấp (tức thời) cho farm {}", farm.getId());
            }
        }

        // 3. Kiểm tra độ ẩm không khí cao
        if (data.getHumidity() != null && data.getHumidity() > HIGH_HUMIDITY_THRESHOLD) {
            if (canSendSensorNotification(farm.getLastHighHumidityWarningAt())) {
                String subject = String.format("[SmartFarm Cảnh Báo] Độ ẩm không khí cao tại %s", farm.getName());
                String text = createSensorEmailText(farm, device, "Độ ẩm không khí", data.getHumidity(), "%",
                        "cao, có nguy cơ phát sinh nấm bệnh", "Hãy tăng cường thông gió và kiểm tra hệ thống quạt.");
                emailService.sendSimpleMessage(ownerEmail, subject, text);
                farm.setLastHighHumidityWarningAt(LocalDateTime.now());
                farmRepository.save(farm);
                log.info("Đã gửi email cảnh báo độ ẩm không khí cao (tức thời) cho farm {}", farm.getId());
            }
        }
    }

    /**
     * Helper method để kiểm tra xem có nên gửi thông báo hay không (dựa trên thời
     * gian cooldown).
     */
    private boolean canSendSensorNotification(LocalDateTime lastNotificationTime) {
        if (lastNotificationTime == null) {
            return true; // Chưa gửi lần nào -> Gửi
        }
        return ChronoUnit.HOURS.between(lastNotificationTime,
                LocalDateTime.now()) >= SENSOR_NOTIFICATION_COOLDOWN_HOURS;
    }

    /**
     * Helper method để tạo nội dung email.
     */
    private String createSensorEmailText(Farm farm, Device device, String metricName, Double value, String unit,
            String issue, String suggestion) {
        return String.format(
                "Xin chào,\n\n" +
                        "Hệ thống SmartFarm giám sát tại nông trại '%s' vừa ghi nhận một thông số bất thường.\n\n" +
                        "--- CHI TIẾT CẢNH BÁO ---\n" +
                        "Thiết bị: %s (ID: %s)\n" +
                        "Chỉ số: %s\n" +
                        "Giá trị đo được: %.1f %s\n" +
                        "Vấn đề: Giá trị này %s.\n" +
                        "Gợi ý: %s\n\n" +
                        "Vui lòng đăng nhập vào hệ thống để theo dõi chi tiết.\n\n" +
                        "Trân trọng,\n" +
                        "Đội ngũ SmartFarm.",
                farm.getName(),
                device.getName(),
                device.getDeviceId(),
                metricName,
                value,
                unit,
                issue,
                suggestion);
    }
}