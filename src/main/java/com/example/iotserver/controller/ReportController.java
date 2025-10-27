package com.example.iotserver.controller;

import com.example.iotserver.dto.response.ApiResponse;
import com.example.iotserver.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "08. Reports", description = "API báo cáo và thống kê")
public class ReportController {

    private final ReportService reportService;

    /**
     * Lấy dữ liệu tóm tắt cho dashboard hoặc báo cáo
     * GET /api/reports/summary?farmId=1
     */
    @GetMapping("/summary")
    @Operation(summary = "Lấy báo cáo tổng hợp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummary(
            @Parameter(description = "ID nông trại") @RequestParam Long farmId) {
        Map<String, Object> summary = reportService.getDashboardSummary(farmId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    // Các API khác cho báo cáo có thể được thêm vào đây
    // Ví dụ: Lấy lịch sử tưới nước, lượng điện tiêu thụ...
    // --- ENDPOINT MỚI CHO CSV ---
    @GetMapping("/export/csv")
    @Operation(summary = "Xuất báo cáo dữ liệu cảm biến ra file CSV")
    @PreAuthorize("isAuthenticated()") // Yêu cầu đăng nhập
    public void exportSensorDataToCsv(
            HttpServletResponse response,
            @Parameter(description = "ID của thiết bị", required = true) @RequestParam String deviceId,
            @Parameter(description = "Thời gian bắt đầu (ISO 8601 format, e.g., 2023-10-27T00:00:00Z)", required = true) @RequestParam Instant start,
            @Parameter(description = "Thời gian kết thúc (ISO 8601 format, e.g., 2023-10-28T00:00:00Z)", required = true) @RequestParam Instant end)
            throws IOException {
        reportService.writeSensorDataToCsv(response, deviceId, start, end);
    }

    // --- ENDPOINT MỚI CHO PDF ---
    @GetMapping("/export/pdf")
    @Operation(summary = "Xuất báo cáo dữ liệu cảm biến ra file PDF")
    @PreAuthorize("isAuthenticated()") // Yêu cầu đăng nhập
    public void exportSensorDataToPdf(
            HttpServletResponse response,
            @Parameter(description = "ID của thiết bị", required = true) @RequestParam String deviceId,
            @Parameter(description = "Thời gian bắt đầu (ISO 8601 format, e.g., 2023-10-27T00:00:00Z)", required = true) @RequestParam Instant start,
            @Parameter(description = "Thời gian kết thúc (ISO 8601 format, e.g., 2023-10-28T00:00:00Z)", required = true) @RequestParam Instant end)
            throws IOException {
        reportService.createSensorDataPdf(response, deviceId, start, end);
    }
}