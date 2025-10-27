package com.example.iotserver.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AIPredictionResponse {
    @JsonProperty("predictions")
    private List<PredictionPoint> predictions;

    @JsonProperty("suggestion")
    private AISuggestion suggestion;

    @JsonProperty("model_info")
    private Map<String, String> modelInfo;

    @Data
    public static class PredictionPoint {
        private String timestamp;
        @JsonProperty("predicted_temperature")
        private Double predictedTemperature;
        @JsonProperty("predicted_humidity")
        private Double predictedHumidity;
        @JsonProperty("predicted_soil_moisture")
        private Double predictedSoilMoisture;
    }

    @Data
    public static class AISuggestion {
        @JsonProperty("action")
        private String action; // e.g., "WATER", "WAIT"
        @JsonProperty("message")
        private String message;
        @JsonProperty("confidence")
        private Double confidence;
        @JsonProperty("details")
        private Map<String, Object> details;
    }
}