package com.example.iotserver.controller;

import com.example.iotserver.dto.DeviceDTO;
import com.example.iotserver.dto.SensorDataDTO;
import com.example.iotserver.dto.response.ApiResponse;
import com.example.iotserver.service.DeviceService;
import com.example.iotserver.service.SensorDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@Tag(name = "02. Device Management", description = "API quản lý thiết bị IoT (cảm biến, actuator)")
public class DeviceController {

    private final DeviceService deviceService;
    private final SensorDataService sensorDataService;

    /**
     * Create new device
     * POST /api/devices?farmId=1
     */
    @PostMapping
    @Operation(summary = "Tạo thiết bị mới", description = "Đăng ký thiết bị IoT mới cho nông trại")
    public ResponseEntity<ApiResponse<DeviceDTO>> createDevice(
            @Parameter(description = "ID nông trại") @RequestParam Long farmId,
            @RequestBody DeviceDTO dto) {
        DeviceDTO created = deviceService.createDevice(farmId, dto);
        return ResponseEntity.ok(ApiResponse.success("Device created successfully", created));
    }

    /**
     * Update device
     * PUT /api/devices/{id}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin thiết bị")
    public ResponseEntity<ApiResponse<DeviceDTO>> updateDevice(
            @PathVariable Long id,
            @RequestBody DeviceDTO dto) {
        DeviceDTO updated = deviceService.updateDevice(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Device updated successfully", updated));
    }

    /**
     * Delete device
     * DELETE /api/devices/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa thiết bị")
    public ResponseEntity<ApiResponse<Void>> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ResponseEntity.ok(ApiResponse.success("Device deleted successfully", null));
    }

    /**
     * Get device by ID
     * GET /api/devices/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin thiết bị theo ID")
    public ResponseEntity<ApiResponse<DeviceDTO>> getDevice(@PathVariable Long id) {
        DeviceDTO device = deviceService.getDevice(id);
        return ResponseEntity.ok(ApiResponse.success(device));
    }

    /**
     * Get device with latest data
     * GET /api/devices/{deviceId}/full
     */
    @GetMapping("/{deviceId}/full")
    @Operation(summary = "Lấy thiết bị kèm dữ liệu cảm biến mới nhất")
    public ResponseEntity<ApiResponse<DeviceDTO>> getDeviceWithData(@PathVariable String deviceId) {
        DeviceDTO device = deviceService.getDeviceWithLatestData(deviceId);
        return ResponseEntity.ok(ApiResponse.success(device));
    }

    /**
     * Get all devices for a farm
     * GET /api/devices?farmId=1
     */
    @GetMapping
    @Operation(summary = "Lấy danh sách thiết bị của nông trại")
    public ResponseEntity<ApiResponse<List<DeviceDTO>>> getDevicesByFarm(
            @Parameter(description = "ID nông trại") @RequestParam Long farmId,
            @Parameter(description = "Loại thiết bị (SENSOR_DHT22, ACTUATOR_PUMP,...)") @RequestParam(required = false) String type,
            @Parameter(description = "Bao gồm dữ liệu cảm biến") @RequestParam(defaultValue = "false") boolean withData) {

        List<DeviceDTO> devices;
        if (type != null) {
            devices = deviceService.getDevicesByFarmAndType(farmId, type);
        } else if (withData) {
            devices = deviceService.getDevicesByFarmWithData(farmId);
        } else {
            devices = deviceService.getDevicesByFarm(farmId);
        }

        return ResponseEntity.ok(ApiResponse.success(devices));
    }

    /**
     * Get online devices
     * GET /api/devices/online?farmId=1
     */
    @GetMapping("/online")
    @Operation(summary = "Lấy danh sách thiết bị đang online")
    public ResponseEntity<ApiResponse<List<DeviceDTO>>> getOnlineDevices(
            @Parameter(description = "ID nông trại") @RequestParam Long farmId) {
        List<DeviceDTO> devices = deviceService.getOnlineDevices(farmId);
        return ResponseEntity.ok(ApiResponse.success(devices));
    }

    /**
     * Control device (turn on/off)
     * POST /api/devices/{deviceId}/control
     * Body: {"action": "turn_on", "duration": 300}
     */
    @PostMapping("/{deviceId}/control")
    @Operation(summary = "Điều khiển thiết bị", description = "Gửi lệnh điều khiển (bật/tắt máy bơm, quạt,...)")
    public ResponseEntity<ApiResponse<Map<String, String>>> controlDevice(
            @PathVariable String deviceId,
            @RequestBody Map<String, Object> command) {
        String action = (String) command.get("action");
        deviceService.controlDevice(deviceId, action, command);

        return ResponseEntity.ok(ApiResponse.success(
                "Command sent successfully",
                Map.of("status", "success", "message", "Command sent to device " + deviceId)));
    }

    /**
     * Get latest sensor data for a device
     * GET /api/devices/{deviceId}/data/latest
     */
    @GetMapping("/{deviceId}/data/latest")
    @Operation(summary = "Lấy dữ liệu cảm biến mới nhất")
    public ResponseEntity<ApiResponse<SensorDataDTO>> getLatestData(@PathVariable String deviceId) {
        SensorDataDTO data = sensorDataService.getLatestSensorData(deviceId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Get sensor data for time range
     * GET /api/devices/{deviceId}/data?start=...&end=...
     */
    @GetMapping("/{deviceId}/data")
    @Operation(summary = "Lấy dữ liệu cảm biến theo khoảng thời gian")
    public ResponseEntity<ApiResponse<List<SensorDataDTO>>> getSensorDataRange(
            @PathVariable String deviceId,
            @Parameter(description = "Thời gian bắt đầu (ISO format)") @RequestParam String start,
            @Parameter(description = "Thời gian kết thúc (ISO format)") @RequestParam String end) {
        Instant startTime = Instant.parse(start);
        Instant endTime = Instant.parse(end);

        List<SensorDataDTO> data = sensorDataService.getSensorDataRange(deviceId, startTime, endTime);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Get aggregated data for charts
     * GET /api/devices/{deviceId}/data/aggregated?field=temperature&window=1h
     */
    @GetMapping("/{deviceId}/data/aggregated")
    @Operation(summary = "Lấy dữ liệu tổng hợp cho biểu đồ")
    public ResponseEntity<ApiResponse<List<SensorDataDTO>>> getAggregatedData(
            @PathVariable String deviceId,
            @Parameter(description = "Trường dữ liệu (temperature, humidity,...)") @RequestParam String field,
            @Parameter(description = "Hàm tổng hợp (mean, max, min)") @RequestParam(defaultValue = "mean") String aggregation,
            @Parameter(description = "Cửa sổ thời gian (1h, 1d,...)") @RequestParam(defaultValue = "1h") String window) {

        try {
            List<SensorDataDTO> data = sensorDataService.getAggregatedData(
                    deviceId, field, aggregation, window);

            // ✅ THÊM: Kiểm tra dữ liệu rỗng
            if (data.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success(
                        "Không có dữ liệu trong 7 ngày qua",
                        Collections.emptyList()));
            }

            return ResponseEntity.ok(ApiResponse.success(data));

        } catch (Exception e) {
            log.error("❌ Lỗi khi lấy aggregated data: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Không thể lấy dữ liệu: " + e.getMessage()));
        }
    }
}
