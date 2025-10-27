package com.example.iotserver.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "farms")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Farm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;
    private String location;
    private Double area;

    // ✅ Child side - Không serialize khi trả về JSON
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    @JsonBackReference
    private User owner;

    // ✅ Parent side cho Zones
    @OneToMany(mappedBy = "farm", cascade = CascadeType.ALL)
    @JsonManagedReference("farm-zones")
    private List<Zone> zones = new ArrayList<>();

    // ✅ Parent side cho Rules
    @OneToMany(mappedBy = "farm", cascade = CascadeType.ALL)
    @JsonManagedReference("farm-rules")
    private List<Rule> rules = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // <<<< THÊM CÁC TRƯỜNG MỚI DƯỚI ĐÂY >>>>
    @Column(name = "last_high_temp_warning_at")
    private LocalDateTime lastHighTempSensorWarningAt;

    @Column(name = "last_low_soil_warning_at")
    private LocalDateTime lastLowSoilMoistureWarningAt;

    @Column(name = "last_high_humidity_warning_at")
    private LocalDateTime lastHighHumidityWarningAt;

    @Column(name = "last_low_light_warning_at")
    private LocalDateTime lastLowLightWarningAt;

    @Column(name = "last_high_ph_warning_at")
    private LocalDateTime lastHighPhWarningAt;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

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