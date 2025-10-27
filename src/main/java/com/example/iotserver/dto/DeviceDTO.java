package com.example.iotserver.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import java.io.Serializable; // <-- THÊM IMPORT NÀY

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String deviceId;
    private String name;
    private String description;
    private String type;
    private String status;

    private Long farmId;
    private String farmName;
    private String farmLocation;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastSeen;

    private String lastSeenHumanReadable;
    private Boolean isOnline;
    private Long uptimeSeconds;

    // ✅ GIỮ CẢ 2 - Cho linh hoạt
    private SensorDataDTO latestSensorData; // Latest sensor data object
    private Map<String, Object> latestData; // Latest data as map (for backward compatibility)

    private Map<String, Object> config;
    private String metadata;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private Long totalDataPoints;
    private LocalDateTime lastDataReceivedAt;

    public void calculateDerivedFields() {
        if (lastSeen != null) {
            Duration duration = Duration.between(lastSeen, LocalDateTime.now());
            long minutes = duration.toMinutes();

            if (minutes < 1) {
                this.lastSeenHumanReadable = "Just now";
            } else if (minutes < 60) {
                this.lastSeenHumanReadable = minutes + " minutes ago";
            } else {
                long hours = duration.toHours();
                this.lastSeenHumanReadable = hours + " hours ago";
            }

            this.isOnline = minutes < 5;
        }
    }
}
