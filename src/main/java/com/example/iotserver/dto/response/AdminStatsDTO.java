// src/main/java/com/example/iotserver/dto/response/AdminStatsDTO.java

package com.example.iotserver.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminStatsDTO {
    private long totalUsers;
    private long totalFarms;
    private long totalDevices;
    private long onlineDevices;
    private long totalRules;
}