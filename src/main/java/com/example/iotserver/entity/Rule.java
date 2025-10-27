package com.example.iotserver.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // Tên quy tắc: "Tưới nước tự động"

    @Column(length = 1000)
    private String description; // Mô tả

    // ✅ Child side - Không serialize khi trả về JSON
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    @JsonBackReference("farm-rules")
    private Farm farm;

    // Quy tắc có kích hoạt không?
    @Column(nullable = false)
    private Boolean enabled = true;

    // Mức độ ưu tiên (số càng lớn càng ưu tiên)
    @Column(nullable = false)
    private Integer priority = 0;

    // Các điều kiện (NẾU...)
    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RuleCondition> conditions = new ArrayList<>();

    // Các hành động (THÌ...)
    @ElementCollection
    @CollectionTable(name = "rule_actions", joinColumns = @JoinColumn(name = "rule_id"))
    @Builder.Default
    private List<RuleAction> actions = new ArrayList<>();

    // Lần cuối chạy
    @Column(name = "last_executed_at")
    private LocalDateTime lastExecutedAt;

    // Số lần đã chạy
    @Column(name = "execution_count")
    @Builder.Default
    private Long executionCount = 0L;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Lớp con cho Action
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RuleAction {

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private ActionType type; // TURN_ON_DEVICE, TURN_OFF_DEVICE, SEND_NOTIFICATION

        @Column(name = "device_id")
        private String deviceId; // Thiết bị cần điều khiển

        @Column(name = "duration_seconds")
        private Integer durationSeconds; // Thời gian (giây) - VD: bật bơm 15 phút = 900 giây

        @Column(length = 500)
        private String message; // Nội dung thông báo
    }

    public enum ActionType {
        TURN_ON_DEVICE, // Bật thiết bị
        TURN_OFF_DEVICE, // Tắt thiết bị
        SEND_NOTIFICATION, // Gửi thông báo
        SEND_EMAIL // Gửi email
    }
}