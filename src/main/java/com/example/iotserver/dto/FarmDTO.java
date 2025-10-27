package com.example.iotserver.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.io.Serializable; // <-- THÊM IMPORT NÀY

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FarmDTO implements Serializable { // <-- THÊM "implements Serializable" VÀO ĐÂY

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String description;
    private String location;
    private Double area;

    // Owner info
    private Long ownerId;
    private String ownerName;
    private String ownerEmail;

    // Statistics
    private Long totalDevices;
    private Long onlineDevices;
    private Long offlineDevices;
    private Long totalZones;

    // Latest environmental data
    private Double avgTemperature;
    private Double avgHumidity;
    private Double avgSoilMoisture;

    // Alerts
    private Integer activeAlerts;
    private List<String> alertTypes;

    // Device summary
    private List<DeviceDTO> recentDevices;

    // Timestamps
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastActivityAt;
}