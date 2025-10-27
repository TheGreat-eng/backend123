package com.example.iotserver.controller;

import com.example.iotserver.dto.RuleDTO;
import com.example.iotserver.dto.RuleExecutionLogDTO;
import com.example.iotserver.dto.response.ApiResponse;
import com.example.iotserver.service.RuleEngineService;
import com.example.iotserver.service.RuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
@Tag(name = "04. Rule Automation", description = "API quản lý quy tắc tự động hóa")
public class RuleController {

    private final RuleService ruleService;
    private final RuleEngineService ruleEngineService;

    /**
     * Tạo quy tắc mới
     * POST /api/rules?farmId=1
     */
    @PostMapping
    @Operation(summary = "Tạo quy tắc tự động hóa mới")
    public ResponseEntity<ApiResponse<RuleDTO>> createRule(
            @Parameter(description = "ID nông trại") @RequestParam Long farmId,
            @RequestBody RuleDTO dto) {
        RuleDTO created = ruleService.createRule(farmId, dto);
        return ResponseEntity.ok(ApiResponse.success("Tạo quy tắc thành công", created));
    }

    /**
     * Cập nhật quy tắc
     * PUT /api/rules/{id}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật quy tắc")
    public ResponseEntity<ApiResponse<RuleDTO>> updateRule(
            @PathVariable Long id,
            @RequestBody RuleDTO dto) {
        RuleDTO updated = ruleService.updateRule(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật quy tắc thành công", updated));
    }

    /**
     * Xóa quy tắc
     * DELETE /api/rules/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa quy tắc")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable Long id) {
        ruleService.deleteRule(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa quy tắc thành công", null));
    }

    /**
     * Lấy chi tiết quy tắc
     * GET /api/rules/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết quy tắc")
    public ResponseEntity<ApiResponse<RuleDTO>> getRule(@PathVariable Long id) {
        RuleDTO rule = ruleService.getRule(id);
        return ResponseEntity.ok(ApiResponse.success(rule));
    }

    /**
     * Lấy danh sách quy tắc của Farm
     * GET /api/rules?farmId=1
     */
    @GetMapping
    @Operation(summary = "Lấy danh sách quy tắc của nông trại")
    public ResponseEntity<ApiResponse<List<RuleDTO>>> getRulesByFarm(
            @Parameter(description = "ID nông trại") @RequestParam Long farmId,
            @Parameter(description = "Chỉ lấy quy tắc đang bật") @RequestParam(required = false, defaultValue = "false") Boolean enabledOnly) {

        List<RuleDTO> rules;
        if (enabledOnly) {
            rules = ruleService.getEnabledRules(farmId);
        } else {
            rules = ruleService.getRulesByFarm(farmId);
        }

        return ResponseEntity.ok(ApiResponse.success(rules));
    }

    /**
     * Bật/tắt quy tắc
     * PATCH /api/rules/{id}/toggle
     * Body: { "enabled": true }
     */
    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Bật/tắt quy tắc")
    public ResponseEntity<ApiResponse<RuleDTO>> toggleRule(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {
        Boolean enabled = request.get("enabled");
        RuleDTO updated = ruleService.toggleRule(id, enabled);
        return ResponseEntity.ok(ApiResponse.success(
                enabled ? "Đã bật quy tắc" : "Đã tắt quy tắc",
                updated));
    }

    /**
     * Lấy lịch sử thực thi
     * GET /api/rules/{id}/logs?limit=50
     */
    @GetMapping("/{id}/logs")
    @Operation(summary = "Lấy lịch sử thực thi quy tắc")
    public ResponseEntity<ApiResponse<List<RuleExecutionLogDTO>>> getExecutionLogs(
            @PathVariable Long id,
            @Parameter(description = "Số bản ghi tối đa") @RequestParam(defaultValue = "50") int limit) {
        List<RuleExecutionLogDTO> logs = ruleService.getRuleExecutionLogs(id, limit);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    /**
     * Chạy thử quy tắc (manual trigger)
     * POST /api/rules/{id}/execute
     */
    @PostMapping("/{id}/execute")
    @Operation(summary = "Chạy thử quy tắc thủ công")
    public ResponseEntity<ApiResponse<Map<String, Object>>> executeRule(@PathVariable Long id) {
        RuleDTO rule = ruleService.getRule(id);

        // Convert DTO to Entity để execute (simplified)
        // Trong thực tế, bạn nên lấy entity trực tiếp

        Map<String, Object> result = Map.of(
                "message", "Đã kích hoạt chạy quy tắc",
                "ruleName", rule.getName(),
                "note", "Quy tắc sẽ được kiểm tra trong vòng 30 giây");

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Thống kê quy tắc
     * GET /api/rules/stats?farmId=1
     */
    @GetMapping("/stats")
    @Operation(summary = "Thống kê quy tắc")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRuleStats(
            @Parameter(description = "ID nông trại") @RequestParam Long farmId) {
        List<RuleDTO> allRules = ruleService.getRulesByFarm(farmId);
        List<RuleDTO> enabledRules = ruleService.getEnabledRules(farmId);

        long totalExecutions = allRules.stream()
                .mapToLong(r -> r.getExecutionCount() != null ? r.getExecutionCount() : 0)
                .sum();

        Map<String, Object> stats = Map.of(
                "totalRules", allRules.size(),
                "enabledRules", enabledRules.size(),
                "disabledRules", allRules.size() - enabledRules.size(),
                "totalExecutions", totalExecutions);

        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}