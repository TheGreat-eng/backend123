package com.example.iotserver.service;

import com.example.iotserver.dto.DeviceDTO;
import com.example.iotserver.dto.SensorDataDTO;
import com.example.iotserver.entity.Device;
import com.example.iotserver.entity.Farm;
import com.example.iotserver.repository.DeviceRepository;
import com.example.iotserver.repository.FarmRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.iotserver.service.WebSocketService; // Thêm import này

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable; // <-- THÊM IMPORT
import org.springframework.cache.annotation.CacheEvict; // <-- THÊM IMPORT
import java.time.temporal.ChronoUnit; // <<<< 1. THÊM IMPORT

@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final FarmRepository farmRepository;
    private final SensorDataService sensorDataService;
    private final WebSocketService webSocketService; // Thêm dependency này
    private final EmailService emailService; // <<<< 2. INJECT EMAILSERVICE

    // ✅ THÊM: Inject MQTT Gateway
    private final MqttGateway mqttGateway;

    @Transactional
    public DeviceDTO createDevice(Long farmId, DeviceDTO dto) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new RuntimeException("Farm not found"));

        String deviceId = dto.getDeviceId() != null ? dto.getDeviceId() : generateDeviceId();

        if (deviceRepository.existsByDeviceId(deviceId)) {
            throw new RuntimeException("Device ID already exists");
        }

        Device device = new Device();
        device.setDeviceId(deviceId);
        device.setName(dto.getName());
        device.setDescription(dto.getDescription());

        // ✅ SỬA: Dùng helper method để map type
        device.setType(parseDeviceType(dto.getType()));

        device.setStatus(Device.DeviceStatus.OFFLINE);
        device.setFarm(farm);
        device.setMetadata(dto.getMetadata());

        Device saved = deviceRepository.save(device);
        log.info("Created device: {} for farm: {}", saved.getDeviceId(), farmId);

        return mapToDetailedDTO(saved);
    }

    @Transactional
    @CacheEvict(value = "devices", key = "#deviceId") // <-- THÊM ANNOTATION NÀY
    public DeviceDTO updateDevice(Long deviceId, DeviceDTO dto) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        if (dto.getName() != null)
            device.setName(dto.getName());
        if (dto.getDescription() != null)
            device.setDescription(dto.getDescription());
        if (dto.getMetadata() != null)
            device.setMetadata(dto.getMetadata());
        if (dto.getStatus() != null) {
            device.setStatus(Device.DeviceStatus.valueOf(dto.getStatus()));
        }

        Device updated = deviceRepository.save(device);
        log.info("Updated device: {}", updated.getDeviceId());

        return mapToDetailedDTO(updated);
    }

    @Transactional
    @CacheEvict(value = "devices", key = "#deviceId") // ✅ SỬA: Đổi thành #deviceId
    public void deleteDevice(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        deviceRepository.delete(device);
        log.info("Deleted device: {}", device.getDeviceId());
    }

    @Cacheable(value = "devices", key = "#deviceId") // <-- THÊM ANNOTATION NÀY
    public DeviceDTO getDevice(Long deviceId) {
        log.info("DATABASE HIT: Lấy thông tin device với ID: {}", deviceId); // Thêm log để kiểm tra
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));
        return mapToDetailedDTO(device);
    }

    public DeviceDTO getDeviceWithLatestData(String deviceId) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        DeviceDTO dto = mapToDetailedDTO(device);

        // Get latest sensor data
        SensorDataDTO latestSensorData = sensorDataService.getLatestSensorData(deviceId);
        dto.setLatestSensorData(latestSensorData);

        return dto;
    }

    public List<DeviceDTO> getDevicesByFarm(Long farmId) {
        return deviceRepository.findByFarmId(farmId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ✅ SỬA: Method này để lấy devices với data dạng Map
    public List<DeviceDTO> getDevicesByFarmWithData(Long farmId) {
        List<Device> devices = deviceRepository.findByFarmId(farmId);

        return devices.stream()
                .map(device -> {
                    DeviceDTO dto = mapToDTO(device);

                    // Get latest sensor data for this device
                    try {
                        SensorDataDTO sensorData = sensorDataService.getLatestSensorData(device.getDeviceId());

                        if (sensorData != null) {
                            // Set as SensorDataDTO object
                            dto.setLatestSensorData(sensorData);

                            // Also convert to Map for backward compatibility
                            Map<String, Object> dataMap = convertSensorDataToMap(sensorData);
                            dto.setLatestData(dataMap);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to get sensor data for device {}: {}",
                                device.getDeviceId(), e.getMessage());
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }

    public List<DeviceDTO> getDevicesByFarmAndType(Long farmId, String type) {
        Device.DeviceType deviceType = Device.DeviceType.valueOf(type);
        return deviceRepository.findByFarmIdAndType(farmId, deviceType)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<DeviceDTO> getOnlineDevices(Long farmId) {
        return deviceRepository.findOnlineDevicesByFarmId(farmId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void controlDevice(String deviceId, String action, Map<String, Object> params) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        if (!isActuator(device.getType())) {
            throw new RuntimeException("Device is not controllable");
        }

        // ✅ GỬI LỆNH QUA MQTT
        String topic = String.format("device/%s/control", deviceId);

        Map<String, Object> command = new HashMap<>();
        command.put("deviceId", deviceId);
        command.put("action", action);
        command.putAll(params);
        command.put("timestamp", LocalDateTime.now().toString());

        try {
            mqttGateway.sendToMqtt(new ObjectMapper().writeValueAsString(command), topic);
            log.info("✅ Đã gửi lệnh MQTT tới device {}: {} with params: {}", deviceId, action, params);
        } catch (Exception e) {
            log.error("❌ Lỗi khi gửi lệnh MQTT: {}", e.getMessage());
            throw new RuntimeException("Failed to send control command", e);
        }
    }

    // SỬA LẠI HÀM NÀY
    @Transactional
    public void checkStaleDevices() {
        // Giả sử 5 phút không có tín hiệu là offline
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        List<Device> staleDevices = deviceRepository.findStaleDevices(threshold);

        for (Device device : staleDevices) {
            if (device.getStatus() == Device.DeviceStatus.ONLINE) {
                device.setStatus(Device.DeviceStatus.OFFLINE);
                deviceRepository.save(device);
                log.warn("Device {} marked as offline due to inactivity", device.getDeviceId());

                // ===> THÊM DÒNG NÀY ĐỂ GỬI WEBSOCKET <===
                webSocketService.sendDeviceStatus(device.getFarm().getId(), device.getDeviceId(), "OFFLINE");
            }
            // <<<< 3. GỌI HÀM GỬI EMAIL >>>>
            sendOfflineNotificationIfNeeded(device);
        }
    }

    // Helper methods
    private String generateDeviceId() {
        return "DEV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private boolean isActuator(Device.DeviceType type) {
        return type == Device.DeviceType.ACTUATOR_PUMP ||
                type == Device.DeviceType.ACTUATOR_FAN ||
                type == Device.DeviceType.ACTUATOR_LIGHT;
    }

    // ✅ THÊM: Helper method to convert SensorDataDTO to Map
    private Map<String, Object> convertSensorDataToMap(SensorDataDTO sensorData) {
        Map<String, Object> map = new HashMap<>();

        if (sensorData.getDeviceId() != null) {
            map.put("deviceId", sensorData.getDeviceId());
        }
        if (sensorData.getSensorType() != null) {
            map.put("sensorType", sensorData.getSensorType());
        }
        if (sensorData.getTemperature() != null) {
            map.put("temperature", sensorData.getTemperature());
        }
        if (sensorData.getHumidity() != null) {
            map.put("humidity", sensorData.getHumidity());
        }
        if (sensorData.getSoilMoisture() != null) {
            map.put("soilMoisture", sensorData.getSoilMoisture());
        }
        if (sensorData.getLightIntensity() != null) {
            map.put("lightIntensity", sensorData.getLightIntensity());
        }
        if (sensorData.getSoilPH() != null) {
            map.put("soilPH", sensorData.getSoilPH());
        }
        if (sensorData.getTimestamp() != null) {
            map.put("timestamp", sensorData.getTimestamp().toString());
        }

        return map;
    }

    private DeviceDTO mapToDTO(Device device) {
        DeviceDTO dto = DeviceDTO.builder()
                .id(device.getId())
                .deviceId(device.getDeviceId())
                .name(device.getName())
                .description(device.getDescription())
                .type(device.getType().name())
                .status(device.getStatus().name())
                .farmId(device.getFarm().getId())
                .farmName(device.getFarm().getName())
                .farmLocation(device.getFarm().getLocation())
                .lastSeen(device.getLastSeen())
                .metadata(device.getMetadata())
                .createdAt(device.getCreatedAt())
                .updatedAt(device.getUpdatedAt())
                .build();

        dto.calculateDerivedFields();
        return dto;
    }

    private DeviceDTO mapToDetailedDTO(Device device) {
        DeviceDTO dto = mapToDTO(device);

        if (device.getMetadata() != null && !device.getMetadata().isEmpty()) {
            Map<String, Object> config = new HashMap<>();
            config.put("metadata", device.getMetadata());
            dto.setConfig(config);
        }

        return dto;
    }

    // <<<< 4. THÊM HÀM MỚI NÀY VÀO CUỐI FILE >>>>
    /**
     * Gửi email thông báo nếu thiết bị offline quá lâu và chưa được thông báo gần
     * đây.
     */
    private void sendOfflineNotificationIfNeeded(Device device) {
        if (device.getStatus() != Device.DeviceStatus.OFFLINE)
            return;

        long minutesOffline = ChronoUnit.MINUTES.between(device.getLastSeen(), LocalDateTime.now());
        if (minutesOffline < 60)
            return; // Phải offline ít nhất 1 giờ

        if (device.getLastOfflineNotificationAt() != null &&
                ChronoUnit.HOURS.between(device.getLastOfflineNotificationAt(), LocalDateTime.now()) < 6) {
            return; // Chỉ gửi lại sau mỗi 6 giờ
        }

        Farm farm = device.getFarm();
        String ownerEmail = farm.getOwner().getEmail();

        if (ownerEmail != null && !ownerEmail.isEmpty()) {
            String subject = String.format("[SmartFarm Cảnh Báo] Thiết bị '%s' đã offline", device.getName());
            String text = String.format(
                    "Xin chào,\n\n" +
                            "Thiết bị '%s' (ID: %s) tại nông trại '%s' đã mất kết nối hơn %d phút.\n\n" +
                            "Lần cuối nhận tín hiệu: %s\n\n" +
                            "Vui lòng kiểm tra nguồn điện và kết nối mạng của thiết bị.\n\n" +
                            "Trân trọng,\n" + "Đội ngũ SmartFarm.",
                    device.getName(), device.getDeviceId(), farm.getName(),
                    minutesOffline, device.getLastSeen().toString());

            emailService.sendSimpleMessage(ownerEmail, subject, text);
            log.info("Đã gửi email cảnh báo offline cho thiết bị {} tới {}", device.getDeviceId(), ownerEmail);

            device.setLastOfflineNotificationAt(LocalDateTime.now());
            deviceRepository.save(device);
        }
    }

    // ✅ THÊM: Helper method để map type linh hoạt
    private Device.DeviceType parseDeviceType(String typeStr) {
        // Map các tên ngắn gọn sang tên đầy đủ
        Map<String, Device.DeviceType> typeMapping = Map.of(
                "DHT22", Device.DeviceType.SENSOR_DHT22,
                "SOIL_MOISTURE", Device.DeviceType.SENSOR_SOIL_MOISTURE,
                "LIGHT", Device.DeviceType.SENSOR_LIGHT,
                "PH", Device.DeviceType.SENSOR_PH,
                "PUMP", Device.DeviceType.ACTUATOR_PUMP,
                "FAN", Device.DeviceType.ACTUATOR_FAN,
                "LIGHT_ACTUATOR", Device.DeviceType.ACTUATOR_LIGHT);

        // Thử tìm trong map trước
        if (typeMapping.containsKey(typeStr)) {
            return typeMapping.get(typeStr);
        }

        // Nếu không có trong map, parse trực tiếp từ enum
        try {
            return Device.DeviceType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid device type: " + typeStr +
                    ". Valid types: " + String.join(", ", Device.DeviceType.values().toString()));
        }
    }
}
