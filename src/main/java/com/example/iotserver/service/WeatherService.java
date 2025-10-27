package com.example.iotserver.service;

import com.example.iotserver.dto.WeatherDTO;
import com.example.iotserver.entity.Farm;
import com.example.iotserver.entity.Weather;
import com.example.iotserver.repository.FarmRepository;
import com.example.iotserver.repository.WeatherRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class WeatherService {

    private final WeatherRepository weatherRepository;
    private final FarmRepository farmRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Value("${openweather.api.key}")
    private String apiKey;

    @Value("${openweather.api.url}")
    private String apiUrl;

    /**
     * Lấy thời tiết hiện tại
     */
    public WeatherDTO getCurrentWeather(Long farmId) {
        // Lấy từ database (cache)
        Weather weather = weatherRepository
                .findTopByFarmIdOrderByRecordedAtDesc(farmId)
                .orElse(null);

        if (weather == null) {
            // Nếu chưa có, fetch từ API
            Farm farm = farmRepository.findById(farmId)
                    .orElseThrow(() -> new RuntimeException("Farm not found"));

            weather = fetchAndSaveWeather(farm);
        }

        return mapToDTO(weather);
    }

    /**
     * Lấy dự báo 5 ngày
     */
    public WeatherDTO getWeatherForecast(Long farmId) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new RuntimeException("Farm not found"));

        WeatherDTO weatherDTO = getCurrentWeather(farmId);

        // Fetch forecast từ API
        List<WeatherDTO.ForecastDTO> forecast = fetchForecast(farm.getLocation());
        weatherDTO.setForecast(forecast);

        return weatherDTO;
    }

    /**
     * Tự động cập nhật mỗi 30 phút
     */
    @Scheduled(fixedRate = 1800000, initialDelay = 60000) // 30 phút
    @Transactional
    public void updateAllWeatherData() {
        log.info("🌤️ Bắt đầu cập nhật thời tiết tự động...");

        List<Farm> farms = farmRepository.findAll();
        int successCount = 0;
        int failCount = 0;

        for (Farm farm : farms) {
            try {
                fetchAndSaveWeather(farm);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("Lỗi khi cập nhật thời tiết cho farm {}: {}",
                        farm.getId(), e.getMessage());
            }
        }

        log.info("✅ Hoàn thành cập nhật thời tiết: {} thành công, {} lỗi",
                successCount, failCount);
    }

    /**
     * Fetch thời tiết từ OpenWeatherMap API
     */
    private Weather fetchAndSaveWeather(Farm farm) {
        try {
            String url = String.format(
                    "%s/weather?q=%s&appid=%s&units=metric&lang=vi",
                    apiUrl, farm.getLocation(), apiKey);

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            // Parse JSON response
            Weather weather = Weather.builder()
                    .farm(farm)
                    .location(farm.getLocation())
                    .temperature(root.path("main").path("temp").asDouble())
                    .humidity(root.path("main").path("humidity").asDouble())
                    .pressure(root.path("main").path("pressure").asDouble())
                    .windSpeed(root.path("wind").path("speed").asDouble())
                    .weatherCondition(root.path("weather").get(0).path("main").asText())
                    .description(root.path("weather").get(0).path("description").asText())
                    .icon(root.path("weather").get(0).path("icon").asText())
                    .rainAmount(root.path("rain").path("1h").asDouble(0.0))
                    .recordedAt(LocalDateTime.now())
                    .build();

            Weather saved = weatherRepository.save(weather);
            log.info("✅ Đã cập nhật thời tiết cho farm {}: {} - {}°C",
                    farm.getId(), weather.getDescription(), weather.getTemperature());

            return saved;

        } catch (Exception e) {
            log.error("❌ Lỗi khi fetch thời tiết: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch weather data", e);
        }
    }

    /**
     * Fetch dự báo 5 ngày
     */
    private List<WeatherDTO.ForecastDTO> fetchForecast(String location) {
        try {
            String url = String.format(
                    "%s/forecast?q=%s&appid=%s&units=metric&lang=vi",
                    apiUrl, location, apiKey);

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode list = root.path("list");

            List<WeatherDTO.ForecastDTO> forecasts = new ArrayList<>();

            // Lấy 8 dự báo đầu tiên (24 giờ, mỗi 3 giờ 1 lần)
            for (int i = 0; i < Math.min(8, list.size()); i++) {
                JsonNode item = list.get(i);

                long timestamp = item.path("dt").asLong();
                LocalDateTime dateTime = LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());

                String icon = item.path("weather").get(0).path("icon").asText();

                WeatherDTO.ForecastDTO forecast = WeatherDTO.ForecastDTO.builder()
                        .dateTime(dateTime)
                        .temperature(item.path("main").path("temp").asDouble())
                        .humidity(item.path("main").path("humidity").asDouble())
                        .rainProbability(item.path("pop").asDouble(0) * 100)
                        .weatherCondition(item.path("weather").get(0).path("main").asText())
                        .description(item.path("weather").get(0).path("description").asText())
                        .icon(icon)
                        .iconUrl(getIconUrl(icon))
                        .build();

                forecasts.add(forecast);
            }

            return forecasts;

        } catch (Exception e) {
            log.error("❌ Lỗi khi fetch forecast: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Map Entity to DTO
     */
    private WeatherDTO mapToDTO(Weather weather) {
        WeatherDTO dto = WeatherDTO.builder()
                .id(weather.getId())
                .farmId(weather.getFarm().getId())
                .location(weather.getLocation())
                .temperature(weather.getTemperature())
                .humidity(weather.getHumidity())
                .pressure(weather.getPressure())
                .windSpeed(weather.getWindSpeed())
                .weatherCondition(weather.getWeatherCondition())
                .description(weather.getDescription())
                .icon(weather.getIcon())
                .iconUrl(getIconUrl(weather.getIcon()))
                .rainAmount(weather.getRainAmount())
                .rainProbability(weather.getRainProbability())
                .uvIndex(weather.getUvIndex())
                .recordedAt(weather.getRecordedAt())
                .build();

        // Thêm gợi ý
        dto.setSuggestion(generateSuggestion(weather));

        return dto;
    }

    /**
     * Tạo URL icon thời tiết
     */
    private String getIconUrl(String icon) {
        return String.format("https://openweathermap.org/img/wn/%s@2x.png", icon);
    }

    /**
     * Tạo gợi ý dựa trên thời tiết
     */
    private String generateSuggestion(Weather weather) {
        List<String> suggestions = new ArrayList<>();

        // Mưa
        if (weather.getRainAmount() != null && weather.getRainAmount() > 5.0) {
            suggestions.add("Dự báo mưa lớn → Tạm dừng tưới nước");
        } else if (weather.getRainAmount() != null && weather.getRainAmount() > 0) {
            suggestions.add("Có mưa nhẹ → Giảm lượng tưới");
        }

        // Nhiệt độ
        if (weather.getTemperature() > 35) {
            suggestions.add("Nắng nóng → Tăng tưới 20%, phun sương");
        } else if (weather.getTemperature() < 15) {
            suggestions.add("Lạnh → Giảm tưới, che chắn cây");
        }

        // Độ ẩm
        if (weather.getHumidity() > 85) {
            suggestions.add("Độ ẩm cao → Tăng thông gió, phòng nấm");
        }

        // Gió
        if (weather.getWindSpeed() != null && weather.getWindSpeed() > 10) {
            suggestions.add("Gió mạnh → Tưới sáng sớm, tránh bay nước");
        }

        return suggestions.isEmpty() ? "Thời tiết bình thường" : String.join("; ", suggestions);
    }

    /**
     * Dọn dẹp dữ liệu cũ (chạy mỗi ngày)
     */
    @Scheduled(cron = "0 0 3 * * ?") // 3:00 AM mỗi ngày
    @Transactional
    public void cleanupOldWeatherData() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        weatherRepository.deleteOldWeatherData(threshold);
        log.info("🧹 Đã xóa dữ liệu thời tiết cũ hơn 7 ngày");
    }
}