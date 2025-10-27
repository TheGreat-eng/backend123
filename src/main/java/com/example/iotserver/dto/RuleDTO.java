package com.example.iotserver.dto;

import com.example.iotserver.entity.Rule;
import com.example.iotserver.entity.RuleCondition;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RuleDTO {

    private Long id;
    private String name;
    private String description;
    private Long farmId;
    private String farmName;
    private Boolean enabled;
    private Integer priority;

    // Danh sách điều kiện
    @Builder.Default
    private List<ConditionDTO> conditions = new ArrayList<>();

    // Danh sách hành động
    @Builder.Default
    private List<ActionDTO> actions = new ArrayList<>();

    // Thống kê
    private Long executionCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastExecutedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // DTO con cho Điều kiện
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConditionDTO {
        private Long id;
        private String type; // SENSOR_VALUE, TIME_RANGE, ...
        private String field; // temperature, humidity, soil_moisture
        private String operator; // GREATER_THAN, LESS_THAN, ...
        private String value; // 30, 50, ...
        private String deviceId; // DEV-ABC123
        private String logicalOperator; // AND, OR
        private Integer orderIndex;
    }

    // DTO con cho Hành động
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionDTO {
        private String type; // TURN_ON_DEVICE, SEND_NOTIFICATION, ...
        private String deviceId; // Thiết bị cần điều khiển
        private Integer durationSeconds; // Thời gian (giây)
        private String message; // Nội dung thông báo
    }
}