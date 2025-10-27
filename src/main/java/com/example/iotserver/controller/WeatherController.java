package com.example.iotserver.controller;

import com.example.iotserver.dto.WeatherDTO;
import com.example.iotserver.dto.response.ApiResponse;
import com.example.iotserver.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
@Tag(name = "06. Weather", description = "API dữ liệu thời tiết")
public class WeatherController {

    private final WeatherService weatherService;

    /**
     * Lấy thời tiết hiện tại
     * GET /api/weather/current?farmId=1
     */
    @GetMapping("/current")
    @Operation(summary = "Lấy thời tiết hiện tại")
    public ResponseEntity<ApiResponse<WeatherDTO>> getCurrentWeather(
            @Parameter(description = "ID nông trại") @RequestParam Long farmId) {
        WeatherDTO weather = weatherService.getCurrentWeather(farmId);
        return ResponseEntity.ok(ApiResponse.success(weather));
    }

    /**
     * Lấy dự báo 5 ngày
     * GET /api/weather/forecast?farmId=1
     */
    @GetMapping("/forecast")
    @Operation(summary = "Lấy dự báo thời tiết 5 ngày")
    public ResponseEntity<ApiResponse<WeatherDTO>> getForecast(
            @Parameter(description = "ID nông trại") @RequestParam Long farmId) {
        WeatherDTO forecast = weatherService.getWeatherForecast(farmId);
        return ResponseEntity.ok(ApiResponse.success(forecast));
    }

    /**
     * Force update thời tiết (manual trigger)
     * POST /api/weather/update?farmId=1
     */
    @PostMapping("/update")
    @Operation(summary = "Cập nhật thời tiết thủ công")
    public ResponseEntity<ApiResponse<String>> forceUpdate(
            @Parameter(description = "ID nông trại") @RequestParam Long farmId) {
        weatherService.updateAllWeatherData();
        return ResponseEntity.ok(ApiResponse.success(
                "Đã cập nhật thời tiết thành công",
                "Updated"));
    }

    /**
     * Kiểm tra ảnh hưởng thời tiết lên Rule
     * GET /api/weather/rule-impact?farmId=1
     */
    @GetMapping("/rule-impact")
    @Operation(summary = "Kiểm tra ảnh hưởng thời tiết lên quy tắc tự động")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRuleImpact(
            @Parameter(description = "ID nông trại") @RequestParam Long farmId) {

        WeatherDTO weather = weatherService.getCurrentWeather(farmId);
        Map<String, Object> impact = new HashMap<>();

        List<String> suggestions = new ArrayList<>();
        boolean shouldStopWatering = false;

        // Kiểm tra mưa
        if (weather.getRainAmount() != null && weather.getRainAmount() > 5.0) {
            suggestions.add("⛔ TẮT HỆ THỐNG TƯỚI - Dự báo mưa lớn");
            shouldStopWatering = true;
        } else if (weather.getRainAmount() != null && weather.getRainAmount() > 0) {
            suggestions.add("⚠️ GIẢM TƯỚI 50% - Có mưa nhẹ");
        }

        // Kiểm tra nắng nóng
        if (weather.getTemperature() > 38) {
            suggestions.add("🔥 TĂNG TƯỚI 20% - Nắng nóng cực đoan");
        }

        // Kiểm tra độ ẩm cao
        if (weather.getHumidity() > 85) {
            suggestions.add("💨 TĂNG THÔNG GIÓ - Nguy cơ nấm");
        }

        impact.put("weather", weather);
        impact.put("shouldStopWatering", shouldStopWatering);
        impact.put("suggestions", suggestions);
        impact.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(ApiResponse.success(impact));
    }
}