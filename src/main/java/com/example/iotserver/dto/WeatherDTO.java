package com.example.iotserver.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WeatherDTO {

    private Long id;
    private Long farmId;
    private String location;

    // Thời tiết hiện tại
    private Double temperature;
    private Double humidity;
    private Double pressure;
    private Double windSpeed;
    private String weatherCondition;
    private String description;
    private String icon;
    private Double rainAmount;
    private Double rainProbability;
    private Double uvIndex;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recordedAt;

    // Dự báo 5 ngày
    private List<ForecastDTO> forecast;

    // Icon URL đầy đủ
    private String iconUrl;

    // Gợi ý dựa trên thời tiết
    private String suggestion;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForecastDTO {
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime dateTime;
        private Double temperature;
        private Double humidity;
        private Double rainProbability;
        private String weatherCondition;
        private String description;
        private String icon;
        private String iconUrl;
    }
}