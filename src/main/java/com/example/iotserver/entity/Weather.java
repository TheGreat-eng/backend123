package com.example.iotserver.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "weather_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Weather {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Thuộc Farm nào
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    // Vị trí
    @Column(nullable = false)
    private String location; // "Hanoi,VN"

    // Thời tiết hiện tại
    @Column(nullable = false)
    private Double temperature; // Nhiệt độ (°C)

    @Column(nullable = false)
    private Double humidity; // Độ ẩm (%)

    private Double pressure; // Áp suất (hPa)

    private Double windSpeed; // Tốc độ gió (m/s)

    private String weatherCondition; // "Clear", "Rain", "Clouds"

    private String description; // "clear sky", "light rain"

    private String icon; // "01d", "10d" - icon code

    // Mưa
    private Double rainAmount; // Lượng mưa (mm)

    private Double rainProbability; // Xác suất mưa (%)

    // UV index
    private Double uvIndex;

    // Thời gian
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (recordedAt == null) {
            recordedAt = LocalDateTime.now();
        }
    }
}