package com.example.iotserver.entity;

// Thêm 2 import này vào đầu file
import com.example.iotserver.enums.DeviceStatus;
import com.example.iotserver.enums.DeviceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "devices")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // Unique identifier for MQTT
    @Column(nullable = false, unique = true)
    @ToString.Include
    private String deviceId;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceType type; // Giữ nguyên, chỉ cần import đúng

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceStatus status = DeviceStatus.OFFLINE;

    // Device thuộc về Farm (nếu business yêu cầu)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    @ToString.Exclude
    private Farm farm;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(name = "last_offline_notification_at") // <<<< THÊM TRƯỜNG MỚI NÀY
    private LocalDateTime lastOfflineNotificationAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Metadata for device configuration (JSON string for flexible config)
    @Column(columnDefinition = "TEXT")
    private String metadata;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
