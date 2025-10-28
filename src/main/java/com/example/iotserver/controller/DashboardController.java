package com.example.iotserver.controller;

import com.example.iotserver.service.DeviceService;
import com.example.iotserver.service.SensorDataService;
import com.example.iotserver.repository.DeviceRepository;
import com.example.iotserver.enums.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "05. Dashboard", description = "API tổng quan và thống kê")
public class DashboardController {

        private final DeviceService deviceService;
        private final SensorDataService sensorDataService;
        private final DeviceRepository deviceRepository;

        /**
         * Get dashboard overview for a farm
         * GET /api/dashboard/farm/{farmId}
         */
        @GetMapping("/farm/{farmId}")
        @Operation(summary = "Lấy tổng quan dashboard cho nông trại")
        public ResponseEntity<Map<String, Object>> getFarmDashboard(@PathVariable Long farmId) {
                Map<String, Object> dashboard = new HashMap<>();

                // Device statistics
                long totalDevices = deviceRepository.countByFarmId(farmId);
                long onlineDevices = deviceRepository.countByFarmIdAndStatus(
                                farmId, DeviceStatus.ONLINE);
                long offlineDevices = totalDevices - onlineDevices;

                dashboard.put("totalDevices", totalDevices);
                dashboard.put("onlineDevices", onlineDevices);
                dashboard.put("offlineDevices", offlineDevices);

                // Latest sensor data for all devices
                Map<String, Map<String, Object>> latestData = sensorDataService.getFarmLatestData(farmId);
                dashboard.put("latestSensorData", latestData);

                // Device list
                dashboard.put("devices", deviceService.getDevicesByFarm(farmId));

                return ResponseEntity.ok(dashboard);
        }

        /**
         * Get real-time stats
         * GET /api/dashboard/stats?farmId=1
         */
        @GetMapping("/stats")
        @Operation(summary = "Lấy thống kê real-time")
        public ResponseEntity<Map<String, Object>> getRealtimeStats(
                        @Parameter(description = "ID nông trại") @RequestParam Long farmId) {
                Map<String, Object> stats = new HashMap<>();

                // Calculate averages from latest data
                Map<String, Map<String, Object>> latestData = sensorDataService.getFarmLatestData(farmId);

                double avgTemperature = latestData.values().stream()
                                .filter(data -> data.containsKey("temperature"))
                                .mapToDouble(data -> (Double) data.get("temperature"))
                                .average()
                                .orElse(0.0);

                double avgHumidity = latestData.values().stream()
                                .filter(data -> data.containsKey("humidity"))
                                .mapToDouble(data -> (Double) data.get("humidity"))
                                .average()
                                .orElse(0.0);

                double avgSoilMoisture = latestData.values().stream()
                                .filter(data -> data.containsKey("soil_moisture"))
                                .mapToDouble(data -> (Double) data.get("soil_moisture"))
                                .average()
                                .orElse(0.0);
                double avgLightIntensity = latestData.values().stream()
                                .filter(data -> data.containsKey("light_intensity"))
                                .mapToDouble(data -> (Double) data.get("light_intensity"))
                                .average()
                                .orElse(0.0);
                double avgSoilPH = latestData.values().stream()
                                .filter(data -> data.containsKey("soilPH"))
                                .mapToDouble(data -> (Double) data.get("soilPH"))
                                .average()
                                .orElse(0.0);

                stats.put("avgTemperature", Math.round(avgTemperature * 10) / 10.0);
                stats.put("avgHumidity", Math.round(avgHumidity * 10) / 10.0);
                stats.put("avgSoilMoisture", Math.round(avgSoilMoisture * 10) / 10.0);
                stats.put("avgLightIntensity", Math.round(avgLightIntensity * 10) / 10.0);
                stats.put("avgSoilPH", Math.round(avgSoilPH * 10) / 10.0);
                stats.put("timestamp", System.currentTimeMillis());

                return ResponseEntity.ok(stats);
        }
}
