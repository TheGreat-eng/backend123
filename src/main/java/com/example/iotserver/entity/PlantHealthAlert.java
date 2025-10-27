package com.example.iotserver.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Entity lưu trữ cảnh báo sức khỏe cây trồng
 * Phát hiện nguy cơ bệnh dựa trên điều kiện môi trường
 */
@Entity
@Table(name = "plant_health_alerts", indexes = {
        @Index(name = "idx_farm_detected", columnList = "farm_id,detected_at"),
        @Index(name = "idx_severity", columnList = "severity"),
        @Index(name = "idx_resolved", columnList = "resolved")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantHealthAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID nông trại
     */
    @Column(name = "farm_id", nullable = false)
    private Long farmId;

    /**
     * Loại cảnh báo
     * FUNGUS: Nguy cơ nấm
     * HEAT_STRESS: Stress nhiệt
     * DROUGHT: Thiếu nước
     * COLD: Lạnh
     * UNSTABLE_MOISTURE: Độ ẩm dao động
     * LOW_LIGHT: Thiếu ánh sáng
     * PH_ABNORMAL: pH bất thường
     */
    @Column(name = "alert_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private AlertType alertType;

    /**
     * Mức độ nghiêm trọng
     * LOW: Thấp
     * MEDIUM: Trung bình
     * HIGH: Cao
     * CRITICAL: Nghiêm trọng
     */
    @Column(name = "severity", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Severity severity;

    /**
     * Mô tả cảnh báo
     * VD: "Nguy cơ nấm cao - Độ ẩm 90%, nhiệt độ 25°C kéo dài 48h"
     */
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * Gợi ý xử lý
     * VD: "Tăng thông gió, giảm tưới, xử lý phun thuốc phòng nấm"
     */
    @Column(name = "suggestion", nullable = false, columnDefinition = "TEXT")
    private String suggestion;

    /**
     * Điểm sức khỏe (0-100)
     * 100: Tuyệt vời
     * 90-99: Tốt
     * 70-89: Khá
     * 50-69: Cảnh báo
     * 0-49: Nghiêm trọng
     */
    @Column(name = "health_score")
    private Integer healthScore;

    /**
     * Điều kiện môi trường khi phát hiện (JSON)
     * VD: {"temperature":28,"humidity":90,"soilMoisture":45}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conditions", columnDefinition = "json")
    private JsonNode conditions;

    /**
     * Thời điểm phát hiện
     */
    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    /**
     * Đã xử lý chưa
     */
    @Column(name = "resolved", nullable = false)
    @Builder.Default
    private Boolean resolved = false;

    /**
     * Thời điểm xử lý
     */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /**
     * Ghi chú xử lý
     */
    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    /**
     * Enum cho loại cảnh báo
     */
    public enum AlertType {
        FUNGUS("Nguy cơ nấm"),
        HEAT_STRESS("Stress nhiệt"),
        DROUGHT("Thiếu nước"),
        COLD("Lạnh"),
        UNSTABLE_MOISTURE("Độ ẩm dao động"),
        LOW_LIGHT("Thiếu ánh sáng"),
        PH_ABNORMAL("pH bất thường");

        private final String displayName;

        AlertType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Enum cho mức độ nghiêm trọng
     */
    public enum Severity {
        LOW("Thấp"),
        MEDIUM("Trung bình"),
        HIGH("Cao"),
        CRITICAL("Nghiêm trọng");

        private final String displayName;

        Severity(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @PrePersist
    protected void onCreate() {
        if (detectedAt == null) {
            detectedAt = LocalDateTime.now();
        }
        if (resolved == null) {
            resolved = false;
        }
    }
}