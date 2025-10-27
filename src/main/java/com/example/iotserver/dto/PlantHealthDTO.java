package com.example.iotserver.dto;

import com.example.iotserver.entity.PlantHealthAlert.AlertType;
import com.example.iotserver.entity.PlantHealthAlert.Severity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO cho thông tin sức khỏe cây trồng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantHealthDTO {

    /**
     * Điểm sức khỏe tổng thể (0-100)
     */
    private Integer healthScore;

    /**
     * Trạng thái sức khỏe
     * EXCELLENT: 90-100
     * GOOD: 70-89
     * WARNING: 50-69
     * CRITICAL: 0-49
     */
    private String status;

    /**
     * Danh sách cảnh báo đang hoạt động
     */
    private List<AlertDTO> activeAlerts;

    /**
     * Điều kiện môi trường hiện tại
     */
    private Map<String, Object> conditions;

    /**
     * Gợi ý tổng quát
     */
    private String overallSuggestion;

    /**
     * Thời điểm phân tích
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime analyzedAt;

    /**
     * Thống kê theo mức độ
     */
    private SeverityStats severityStats;

    /**
     * DTO cho từng cảnh báo
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertDTO {
        private Long id;
        private AlertType type;
        private String typeName;
        private Severity severity;
        private String severityName;
        private String description;
        private String suggestion;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime detectedAt;

        private Map<String, Object> conditions;
    }

    /**
     * DTO cho thống kê mức độ nghiêm trọng
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeverityStats {
        private long critical;
        private long high;
        private long medium;
        private long low;
        private long total;
    }

    /**
     * Enum cho trạng thái sức khỏe
     */
    public enum HealthStatus {
        EXCELLENT("Tuyệt vời", 90),
        GOOD("Tốt", 70),
        WARNING("Cảnh báo", 50),
        CRITICAL("Nghiêm trọng", 0);

        private final String displayName;
        private final int minScore;

        HealthStatus(String displayName, int minScore) {
            this.displayName = displayName;
            this.minScore = minScore;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getMinScore() {
            return minScore;
        }

        /**
         * Xác định trạng thái dựa trên điểm
         */
        public static HealthStatus fromScore(int score) {
            if (score >= 90)
                return EXCELLENT;
            if (score >= 70)
                return GOOD;
            if (score >= 50)
                return WARNING;
            return CRITICAL;
        }
    }
}