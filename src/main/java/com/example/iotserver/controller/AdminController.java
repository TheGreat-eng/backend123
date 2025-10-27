// src/main/java/com/example/iotserver/controller/AdminController.java

package com.example.iotserver.controller;

import com.example.iotserver.dto.DeviceDTO;
import com.example.iotserver.dto.FarmDTO;
import com.example.iotserver.dto.request.SetPasswordRequest;
import com.example.iotserver.dto.request.UpdateUserRequest;
import com.example.iotserver.dto.response.AdminStatsDTO;
import com.example.iotserver.dto.response.AdminUserDTO;
import com.example.iotserver.dto.response.ApiResponse;
import com.example.iotserver.entity.Device;
import com.example.iotserver.entity.Farm;
import com.example.iotserver.entity.User;
import com.example.iotserver.repository.DeviceRepository;
import com.example.iotserver.repository.FarmRepository;
import com.example.iotserver.repository.RuleRepository;
import com.example.iotserver.repository.UserRepository;
import com.example.iotserver.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "01. Admin Management", description = "API quản trị hệ thống (Yêu cầu quyền ADMIN)")
@SecurityRequirement(name = "bearerAuth") // Yêu cầu xác thực JWT cho tất cả API trong controller này
@PreAuthorize("hasAuthority('ADMIN')") // Chỉ user có role ADMIN mới được truy cập
public class AdminController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final FarmRepository farmRepository;
    private final DeviceRepository deviceRepository;
    private final RuleRepository ruleRepository;

    @GetMapping("/users")
    @Operation(summary = "Lấy danh sách tất cả người dùng (Phân trang)")
    public ResponseEntity<ApiResponse<Page<AdminUserDTO>>> getAllUsers(
            @Parameter(description = "Số trang, bắt đầu từ 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Kích thước trang") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<User> userPage = userService.findAllUsers(pageable);
        Page<AdminUserDTO> userDtoPage = userPage.map(this::convertToAdminUserDTO);
        return ResponseEntity.ok(ApiResponse.success(userDtoPage));
    }

    @PostMapping("/users/{id}/lock")
    @Operation(summary = "Khóa tài khoản người dùng")
    public ResponseEntity<ApiResponse<AdminUserDTO>> lockUser(@PathVariable Long id) {
        User lockedUser = userService.lockUser(id);
        return ResponseEntity.ok(ApiResponse.success("Khóa tài khoản thành công", convertToAdminUserDTO(lockedUser)));
    }

    @PostMapping("/users/{id}/unlock")
    @Operation(summary = "Mở khóa tài khoản người dùng")
    public ResponseEntity<ApiResponse<AdminUserDTO>> unlockUser(@PathVariable Long id) {
        User unlockedUser = userService.unlockUser(id);
        return ResponseEntity
                .ok(ApiResponse.success("Mở khóa tài khoản thành công", convertToAdminUserDTO(unlockedUser)));
    }

    @GetMapping("/stats")
    @Operation(summary = "Lấy thống kê toàn bộ hệ thống")
    public ResponseEntity<ApiResponse<AdminStatsDTO>> getSystemStats() {
        AdminStatsDTO stats = AdminStatsDTO.builder()
                .totalUsers(userRepository.count())
                .totalFarms(farmRepository.count())
                .totalDevices(deviceRepository.count())
                // SỬA DÒNG NÀY
                .onlineDevices(deviceRepository.countByStatus(Device.DeviceStatus.ONLINE))
                .totalRules(ruleRepository.count())
                .build();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // Helper method để chuyển đổi User -> AdminUserDTO
    private AdminUserDTO convertToAdminUserDTO(User user) {
        return AdminUserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }

    // VVVV--- THÊM CÁC API MỚI DƯỚI ĐÂY ---VVVV

    @GetMapping("/farms")
    @Operation(summary = "Lấy danh sách TẤT CẢ nông trại trong hệ thống")
    public ResponseEntity<ApiResponse<List<FarmDTO>>> getAllFarms() {
        List<Farm> farms = farmRepository.findAll();
        List<FarmDTO> farmDtos = farms.stream().map(this::convertToFarmDTO).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(farmDtos));
    }

    @GetMapping("/devices")
    @Operation(summary = "Lấy danh sách TẤT CẢ thiết bị trong hệ thống")
    public ResponseEntity<ApiResponse<List<DeviceDTO>>> getAllDevices() {
        List<Device> devices = deviceRepository.findAll();
        List<DeviceDTO> deviceDtos = devices.stream().map(this::convertToDeviceDTO).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(deviceDtos));
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "Cập nhật thông tin người dùng (Admin)")
    public ResponseEntity<ApiResponse<AdminUserDTO>> updateUserAsAdmin(@PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        User updatedUser = userService.updateUserAsAdmin(id, request);
        return ResponseEntity
                .ok(ApiResponse.success("Cập nhật người dùng thành công", convertToAdminUserDTO(updatedUser)));
    }

    @PostMapping("/users/{id}/set-password")
    @Operation(summary = "Đặt lại mật khẩu cho người dùng (Admin)")
    public ResponseEntity<ApiResponse<String>> setPasswordAsAdmin(@PathVariable Long id,
            @Valid @RequestBody SetPasswordRequest request) {
        userService.setPasswordAsAdmin(id, request);
        return ResponseEntity.ok(ApiResponse.success("Đặt lại mật khẩu thành công", "Success"));
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Xóa mềm một người dùng (Admin)")
    public ResponseEntity<ApiResponse<String>> softDeleteUser(@PathVariable Long id) {
        userService.softDeleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa mềm người dùng thành công", "Success"));
    }

    // ... phương thức convertToAdminUserDTO giữ nguyên ...

    // VVVV--- THÊM CÁC HELPER METHOD ĐỂ MAP DTO ---VVVV
    private FarmDTO convertToFarmDTO(Farm farm) {
        return FarmDTO.builder()
                .id(farm.getId())
                .name(farm.getName())
                .location(farm.getLocation())
                .ownerId(farm.getOwner().getId())
                .ownerEmail(farm.getOwner().getEmail())
                .createdAt(farm.getCreatedAt())
                .totalDevices(deviceRepository.countByFarmId(farm.getId()))
                .build();
    }

    private DeviceDTO convertToDeviceDTO(Device device) {
        DeviceDTO dto = DeviceDTO.builder()
                .id(device.getId())
                .deviceId(device.getDeviceId())
                .name(device.getName())
                .type(device.getType().name())
                .status(device.getStatus().name())
                .farmId(device.getFarm().getId())
                .farmName(device.getFarm().getName())
                .lastSeen(device.getLastSeen())
                .build();
        dto.calculateDerivedFields();
        return dto;
    }
}