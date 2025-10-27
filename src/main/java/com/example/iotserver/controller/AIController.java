package com.example.iotserver.controller;

import com.example.iotserver.dto.AIPredictionResponse;
import com.example.iotserver.dto.response.ApiResponse;
import com.example.iotserver.service.AIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "07. AI Predictions", description = "API dự đoán AI (Machine Learning)")
public class AIController {

    private final AIService aiService;

    @GetMapping("/predictions")
    @Operation(summary = "Lấy dự đoán từ AI/ML model")
    public ResponseEntity<ApiResponse<AIPredictionResponse>> getAIPredictions(
            @Parameter(description = "ID nông trại") @RequestParam Long farmId) {
        AIPredictionResponse predictions = aiService.getPredictions(farmId);
        if (predictions == null) {
            return ResponseEntity.status(503).body(ApiResponse.error("AI Service không khả dụng"));
        }
        return ResponseEntity.ok(ApiResponse.success("Lấy dữ liệu dự đoán thành công", predictions));
    }

    @PostMapping("/diagnose")
    @Operation(summary = "Chẩn đoán bệnh thực vật từ hình ảnh")
    public ResponseEntity<ApiResponse<Map<String, Object>>> diagnoseDisease(
            @Parameter(description = "File ảnh cây bị bệnh") @RequestParam("image") MultipartFile imageFile) {

        if (imageFile.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng tải lên một file ảnh."));
        }

        Map<String, Object> result = aiService.diagnosePlantDisease(imageFile);

        if (result.containsKey("error")) {
            return ResponseEntity.status(503).body(ApiResponse.error((String) result.get("error")));
        }

        return ResponseEntity.ok(ApiResponse.success("Chẩn đoán thành công", result));
    }
}