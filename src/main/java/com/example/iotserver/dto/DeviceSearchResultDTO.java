package com.example.iotserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceSearchResultDTO {
    private Long id;
    private String deviceId;
    private String name;
    private Long farmId; // Thêm farmId để có thể điều hướng
    private String farmName; // Thêm tên farm để hiển thị
}