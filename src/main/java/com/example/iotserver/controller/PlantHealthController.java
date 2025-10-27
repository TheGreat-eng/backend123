package com.example.iotserver.controller;

import com.example.iotserver.dto.PlantHealthDTO;
import com.example.iotserver.entity.PlantHealthAlert;
import com.example.iotserver.service.PlantHealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ... imports
import com.example.iotserver.dto.response.ApiResponse; // <-- THÊM IMPORT NÀY

/**
 * Controller xử lý các API liên quan đến sức khỏe cây trồng
 */
@RestController
@RequestMapping("/api/plant-health")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "09. Plant Health", description = "API quản lý cảnh báo sức khỏe cây trồng")
public class PlantHealthController {

    private final PlantHealthService plantHealthService;

    /**
     * API 1: Lấy tình trạng sức khỏe hiện tại
     * GET /api/plant-health/current?farmId=1
     */
    @GetMapping("/current")
    @Operation(summary = "Lấy tình trạng sức khỏe hiện tại", description = "Phân tích sức khỏe cây dựa trên dữ liệu cảm biến mới nhất và các cảnh báo chưa xử lý")
    public ResponseEntity<ApiResponse<PlantHealthDTO>> getCurrentHealth(
            @Parameter(description = "ID nông trại", required = true) @RequestParam Long farmId) {
        log.info("🌿 [API] Lấy sức khỏe hiện tại cho nông trại: {}", farmId);

        try {
            PlantHealthDTO healthReport = plantHealthService.analyzeHealth(farmId);

            log.info("✅ [API] Điểm sức khỏe: {}, Trạng thái: {}, Số cảnh báo: {}",
                    healthReport.getHealthScore(),
                    healthReport.getStatus(),
                    healthReport.getActiveAlerts().size());

            return ResponseEntity.ok(ApiResponse.success("Lấy dữ liệu sức khỏe thành công", healthReport));

        } catch (Exception e) {
            log.error("❌ [API] Lỗi khi lấy sức khỏe: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi máy chủ khi phân tích sức khỏe."));
        }
    }

    /**
     * API 2: Lấy lịch sử cảnh báo
     * GET /api/plant-health/history?farmId=1&days=7
     */
    @GetMapping("/history")
    @Operation(summary = "Lấy lịch sử cảnh báo", description = "Xem tất cả cảnh báo trong N ngày gần đây")
    public ResponseEntity<Map<String, Object>> getAlertHistory(
            @Parameter(description = "ID nông trại", required = true) @RequestParam Long farmId,

            @Parameter(description = "Số ngày lấy lịch sử (mặc định 7 ngày)") @RequestParam(defaultValue = "7") int days) {
        log.info("📋 [API] Lấy lịch sử cảnh báo cho nông trại {} trong {} ngày", farmId, days);

        try {
            List<PlantHealthAlert> alerts = plantHealthService.getAlertHistory(farmId, days);

            // Thống kê
            Map<String, Long> stats = new HashMap<>();
            stats.put("total", (long) alerts.size());
            stats.put("resolved", alerts.stream().filter(PlantHealthAlert::getResolved).count());
            stats.put("unresolved", alerts.stream().filter(a -> !a.getResolved()).count());

            Map<String, Object> response = new HashMap<>();
            response.put("farmId", farmId);
            response.put("days", days);
            response.put("stats", stats);
            response.put("alerts", alerts);

            log.info("✅ [API] Tìm thấy {} cảnh báo trong {} ngày", alerts.size(), days);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ [API] Lỗi khi lấy lịch sử: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * API 3: Phân tích chi tiết
     * GET /api/plant-health/analyze?farmId=1
     */
    @GetMapping("/analyze")
    @Operation(summary = "Phân tích chi tiết sức khỏe", description = "Chạy lại toàn bộ 7 quy tắc và tạo báo cáo chi tiết")
    public ResponseEntity<Map<String, Object>> analyzeDetailed(
            @Parameter(description = "ID nông trại", required = true) @RequestParam Long farmId) {
        log.info("🔬 [API] Phân tích chi tiết cho nông trại: {}", farmId);

        try {
            // Chạy phân tích
            PlantHealthDTO healthReport = plantHealthService.analyzeHealth(farmId);

            // Lấy lịch sử 7 ngày để so sánh xu hướng
            List<PlantHealthAlert> recentHistory = plantHealthService.getAlertHistory(farmId, 7);

            // Tính xu hướng
            String trend = calculateTrend(healthReport, recentHistory);

            Map<String, Object> response = new HashMap<>();
            response.put("currentHealth", healthReport);
            response.put("recentHistory", recentHistory);
            response.put("trend", trend);
            response.put("recommendations", generateRecommendations(healthReport));

            log.info("✅ [API] Hoàn thành phân tích chi tiết. Xu hướng: {}", trend);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ [API] Lỗi khi phân tích: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * API 4: Đánh dấu cảnh báo đã xử lý
     * POST /api/plant-health/resolve/{alertId}
     */
    @PostMapping("/resolve/{alertId}")
    @Operation(summary = "Đánh dấu cảnh báo đã xử lý", description = "Đánh dấu một cảnh báo là đã được xử lý")
    public ResponseEntity<Map<String, Object>> resolveAlert(
            @Parameter(description = "ID cảnh báo", required = true) @PathVariable Long alertId,

            @Parameter(description = "Ghi chú xử lý") @RequestBody(required = false) Map<String, String> request) {
        log.info("✅ [API] Đánh dấu cảnh báo {} đã xử lý", alertId);

        try {
            String resolutionNote = request != null ? request.get("note") : null;

            plantHealthService.resolveAlert(alertId, resolutionNote);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã đánh dấu cảnh báo là đã xử lý");
            response.put("alertId", alertId);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("❌ [API] Không tìm thấy cảnh báo: {}", e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("❌ [API] Lỗi khi xử lý cảnh báo: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Tính xu hướng sức khỏe (cải thiện/giảm/ổn định)
     */
    private String calculateTrend(PlantHealthDTO current, List<PlantHealthAlert> history) {
        if (history.isEmpty()) {
            return "stable";
        }

        // Đếm số cảnh báo trong 24h gần nhất vs 24h trước đó
        long recent24h = history.stream()
                .filter(a -> a.getDetectedAt().isAfter(
                        java.time.LocalDateTime.now().minusDays(1)))
                .count();

        long previous24h = history.stream()
                .filter(a -> a.getDetectedAt().isBefore(
                        java.time.LocalDateTime.now().minusDays(1))
                        && a.getDetectedAt().isAfter(
                                java.time.LocalDateTime.now().minusDays(2)))
                .count();

        if (recent24h < previous24h) {
            return "improving"; // Đang cải thiện
        } else if (recent24h > previous24h) {
            return "declining"; // Đang giảm
        } else {
            return "stable"; // Ổn định
        }
    }

    /**
     * Tạo khuyến nghị dựa trên báo cáo
     */
    private List<String> generateRecommendations(PlantHealthDTO health) {
        List<String> recommendations = new java.util.ArrayList<>();

        if (health.getHealthScore() >= 90) {
            recommendations.add("✅ Sức khỏe cây tuyệt vời! Tiếp tục duy trì chế độ chăm sóc hiện tại.");
        } else if (health.getHealthScore() >= 70) {
            recommendations.add("👍 Sức khỏe cây tốt. Theo dõi và xử lý các vấn đề nhỏ kịp thời.");
        } else if (health.getHealthScore() >= 50) {
            recommendations.add("⚠️ Cần chú ý! Xử lý các cảnh báo mức HIGH và MEDIUM trong 24-48h.");
        } else {
            recommendations.add("🚨 KHẨN CẤP! Cần xử lý NGAY các vấn đề nghiêm trọng!");
        }

        // Thêm khuyến nghị cụ thể dựa trên loại cảnh báo
        if (health.getActiveAlerts() != null) {
            long fungusCount = health.getActiveAlerts().stream()
                    .filter(a -> a.getType().name().equals("FUNGUS")).count();
            if (fungusCount > 0) {
                recommendations.add("🍄 Tăng cường thông gió và kiểm soát độ ẩm để ngăn nấm phát triển.");
            }

            long droughtCount = health.getActiveAlerts().stream()
                    .filter(a -> a.getType().name().equals("DROUGHT")).count();
            if (droughtCount > 0) {
                recommendations.add("💧 Điều chỉnh lịch tưới để đảm bảo độ ẩm đất ổn định.");
            }

            long heatCount = health.getActiveAlerts().stream()
                    .filter(a -> a.getType().name().equals("HEAT_STRESS")).count();
            if (heatCount > 0) {
                recommendations.add("🔥 Bật hệ thống làm mát hoặc che chắn trong giờ nắng gắt.");
            }
        }

        return recommendations;
    }
}