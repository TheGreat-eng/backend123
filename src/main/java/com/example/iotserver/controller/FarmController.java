package com.example.iotserver.controller;

import com.example.iotserver.dto.FarmDTO;
import com.example.iotserver.entity.User;
import com.example.iotserver.service.FarmService;
import com.example.iotserver.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/farms")
@RequiredArgsConstructor
@Tag(name = "03. Farm Management", description = "API quản lý nông trại")
public class FarmController {

    private final FarmService farmService;
    private final UserService userService;

    /**
     * Create new farm
     * POST /api/farms
     */
    @PostMapping
    @Operation(summary = "Tạo nông trại mới")
    public ResponseEntity<FarmDTO> createFarm(
            @RequestBody FarmDTO dto,
            Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        FarmDTO created = farmService.createFarm(userId, dto);
        return ResponseEntity.ok(created);
    }

    /**
     * Update farm
     * PUT /api/farms/{id}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin nông trại")
    public ResponseEntity<FarmDTO> updateFarm(
            @PathVariable Long id,
            @RequestBody FarmDTO dto,
            Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        FarmDTO updated = farmService.updateFarm(id, userId, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete farm
     * DELETE /api/farms/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa nông trại")
    public ResponseEntity<Void> deleteFarm(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        farmService.deleteFarm(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get farm by ID
     * GET /api/farms/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin nông trại theo ID")
    public ResponseEntity<FarmDTO> getFarm(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        FarmDTO farm = farmService.getFarm(id, userId);
        return ResponseEntity.ok(farm);
    }

    /**
     * Get all farms owned by current user
     * GET /api/farms
     */
    @GetMapping
    @Operation(summary = "Lấy danh sách nông trại của người dùng hiện tại")
    public ResponseEntity<List<FarmDTO>> getUserFarms(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        List<FarmDTO> farms = farmService.getUserFarms(userId);
        return ResponseEntity.ok(farms);
    }

    /**
     * Get all farms user has access to (owned + member)
     * GET /api/farms/accessible
     */
    @GetMapping("/accessible")
    @Operation(summary = "Lấy tất cả nông trại có quyền truy cập")
    public ResponseEntity<List<FarmDTO>> getAccessibleFarms(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        List<FarmDTO> farms = farmService.getFarmsWithAccess(userId);
        return ResponseEntity.ok(farms);
    }

    // ✅ FIX: Helper method to extract user ID from JWT authentication
    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        // Lấy email từ JWT token (principal)
        String email = authentication.getName();

        // Tìm user by email
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        return user.getId();
    }
}