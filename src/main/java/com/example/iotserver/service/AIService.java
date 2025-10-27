// File: src/main/java/com/example/iotserver/service/AIService.java

package com.example.iotserver.service;

import com.example.iotserver.dto.AIPredictionResponse;
import com.example.iotserver.dto.SensorDataDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AIService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final SensorDataService sensorDataService;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    public AIPredictionResponse getPredictions(Long farmId) {
        try {
            // Lấy dữ liệu cảm biến mới nhất (dùng làm current_data)
            SensorDataDTO currentData = sensorDataService.getLatestSensorDataByFarmId(farmId);
            if (currentData == null) {
                log.warn("Không có dữ liệu 'hiện tại' cho farm {} để gửi tới AI", farmId);
                return null;
            }

            // ✅ SỬA: Lấy dữ liệu lịch sử 60 phút trước thay vì dùng current_data
            Instant targetTime = Instant.now().minus(60, ChronoUnit.MINUTES);
            Instant windowStart = targetTime.minus(5, ChronoUnit.MINUTES);
            Instant windowEnd = targetTime.plus(5, ChronoUnit.MINUTES);

            SensorDataDTO historicalData = sensorDataService.getSensorDataRange(
                    currentData.getDeviceId(),
                    windowStart,
                    windowEnd)
                    .stream()
                    .findFirst()
                    .orElse(currentData);

            // Nếu không có dữ liệu lịch sử, dùng current_data làm fallback
            if (historicalData == null) {
                log.warn("Không có dữ liệu lịch sử 60 phút trước, dùng current_data làm fallback");
                historicalData = currentData;
            }

            // Xây dựng request body đúng chuẩn API Python yêu cầu
            Map<String, Object> requestBody = new HashMap<>();

            // Phần current_data
            Map<String, Object> currentDataMap = Map.of(
                    "temperature", currentData.getTemperature(),
                    "humidity", currentData.getHumidity(),
                    "lightIntensity", currentData.getLightIntensity());
            requestBody.put("current_data", currentDataMap);

            // Phần historical_data
            Map<String, Object> historicalDataMap = Map.of(
                    "soilMoisture_lag_60", historicalData.getSoilMoisture(),
                    "temperature_lag_60", historicalData.getTemperature(),
                    "soilMoisture_rolling_mean_60m", historicalData.getSoilMoisture(),
                    "temperature_rolling_mean_60m", historicalData.getTemperature(),
                    "lightIntensity_rolling_mean_60m", historicalData.getLightIntensity());
            // DÒNG SỬA LỖI NẰM Ở ĐÂY:
            requestBody.put("historical_data", historicalDataMap); // Gửi đi Map thay vì DTO

            // Gọi đến đúng endpoint /predict/soil_moisture
            String predictionUrl = aiServiceUrl.replace("/predict", "/predict/soil_moisture");

            log.info("Đang gửi request tới AI Service: {}", predictionUrl);
            AIPredictionResponse response = restTemplate.postForObject(
                    predictionUrl,
                    requestBody,
                    AIPredictionResponse.class);

            log.info("✅ Nhận được phản hồi từ AI Service");
            return response;

        } catch (Exception e) {
            log.error("❌ Lỗi khi gọi AI Service: {}", e.getMessage());
            return null;
        }
    }

    public Map<String, Object> diagnosePlantDisease(MultipartFile imageFile) {
        try {
            String diagnoseUrl = aiServiceUrl.replace("/predict", "/diagnose");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("image", new ByteArrayResource(imageFile.getBytes()) {
                @Override
                public String getFilename() {
                    return imageFile.getOriginalFilename();
                }
            });

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            log.info("Đang gửi ảnh tới AI Service để chẩn đoán: {}", diagnoseUrl);
            Map<String, Object> response = restTemplate.postForObject(diagnoseUrl, requestEntity, Map.class);
            log.info("✅ Nhận được kết quả chẩn đoán từ AI Service");
            return response;

        } catch (IOException e) {
            log.error("❌ Lỗi đọc file ảnh: {}", e.getMessage());
            return Map.of("error", "Lỗi đọc file ảnh");
        } catch (Exception e) {
            log.error("❌ Lỗi khi gọi AI Service chẩn đoán: {}", e.getMessage());
            return Map.of("error", "Lỗi dịch vụ AI không khả dụng");
        }
    }
}