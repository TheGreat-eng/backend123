package com.example.iotserver.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.io.Serializable; // <-- THÊM IMPORT NÀY

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SensorDataDTO implements Serializable { // <-- THÊM "implements Serializable" VÀO ĐÂY

    private static final long serialVersionUID = 1L;

    private String deviceId;
    private String deviceName;
    private String sensorType;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime localTime;

    // Sensor values
    private Double temperature;
    private Double humidity;
    private Double soilMoisture;
    private Double lightIntensity;
    private Double soilPH;

    // Additional metadata
    private Long farmId;
    private String farmName;
    private String location;
    private String unit;

    // Aggregated data fields
    private Double minValue;
    private Double maxValue;
    private Double avgValue;
    private Long count;

    // Flexible values map for custom sensors
    @Builder.Default
    private Map<String, Object> additionalValues = new HashMap<>();

    // Helper method to create from MQTT payload
    public static SensorDataDTO fromMqttPayload(String deviceId, Map<String, Object> payload) {
        SensorDataDTOBuilder builder = SensorDataDTO.builder()
                .deviceId(deviceId)
                .timestamp(Instant.now())
                .localTime(LocalDateTime.now());

        if (payload.containsKey("temperature")) {
            builder.temperature(parseDouble(payload.get("temperature")));
        }
        if (payload.containsKey("humidity")) {
            builder.humidity(parseDouble(payload.get("humidity")));
        }
        if (payload.containsKey("soilMoisture")) {
            builder.soilMoisture(parseDouble(payload.getOrDefault("soilMoisture", payload.get("soil_moisture"))));
        }
        if (payload.containsKey("lightIntensity")) {
            builder.lightIntensity(parseDouble(payload.getOrDefault("lightIntensity", payload.get("light_intensity"))));
        }
        if (payload.containsKey("soilPH")) {
            builder.soilPH(parseDouble(payload.getOrDefault("soilPH", payload.get("soil_ph"))));
        }
        if (payload.containsKey("sensorType")) {
            builder.sensorType(payload.get("sensorType").toString());
        }

        return builder.build();
    }

    public static SensorDataDTO fromInfluxRecord(Map<String, Object> record) {
        SensorDataDTOBuilder builder = SensorDataDTO.builder();

        if (record.containsKey("device_id")) {
            builder.deviceId(record.get("device_id").toString());
        }
        if (record.containsKey("sensor_type")) {
            builder.sensorType(record.get("sensor_type").toString());
        }
        if (record.containsKey("farm_id")) {
            builder.farmId(Long.parseLong(record.get("farm_id").toString()));
        }
        if (record.containsKey("_time")) {
            builder.timestamp(Instant.parse(record.get("_time").toString()));
        }
        if (record.containsKey("_value")) {
            Double value = parseDouble(record.get("_value"));
            String field = record.getOrDefault("_field", "").toString();

            switch (field) {
                case "temperature" -> builder.temperature(value);
                case "humidity" -> builder.humidity(value);
                case "soil_moisture" -> builder.soilMoisture(value);
                case "light_intensity" -> builder.lightIntensity(value);
                case "soilPH" -> builder.soilPH(value);
            }
        }

        return builder.build();
    }

    private static Double parseDouble(Object value) {
        if (value == null)
            return null;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
