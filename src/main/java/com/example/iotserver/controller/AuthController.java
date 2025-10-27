package com.example.iotserver.controller;

import com.example.iotserver.dto.request.LoginRequest;
import com.example.iotserver.dto.request.RegisterRequest;
import com.example.iotserver.dto.response.AuthResponse;
import com.example.iotserver.entity.User;
import com.example.iotserver.enums.UserRole;
import com.example.iotserver.security.JwtUtil;
import com.example.iotserver.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "01. Authentication", description = "API cho việc Đăng ký và Đăng nhập")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "Đăng ký tài khoản mới", description = "Tạo một tài khoản người dùng mới trong hệ thống.") // <--
                                                                                                                    // THÊM
    @ApiResponses(value = { // <-- THÊM
            @ApiResponse(responseCode = "200", description = "Đăng ký thành công", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc Email đã tồn tại", content = @Content)
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        // Kiểm tra email đã tồn tại
        if (userService.existsByEmail(request.getEmail())) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Email đã được sử dụng");
            return ResponseEntity.badRequest().body(error);
        }

        // Tạo user mới
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getEmail()); // ✅ FIX: Thêm username
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhone());
        user.setRole(UserRole.FARMER); // ✅ FIX: Đổi thành UserRole.FARMER

        User savedUser = userService.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Đăng ký thành công");
        response.put("userId", savedUser.getId());
        response.put("email", savedUser.getEmail());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Đăng nhập vào hệ thống")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Đăng nhập thành công"),
            @ApiResponse(responseCode = "400", description = "Sai email hoặc mật khẩu")
    })
    @PostMapping("/login")
    @Transactional
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Email hoặc mật khẩu không đúng");
        }

        // ✅ Tạo access token và refresh token
        String accessToken = jwtUtil.generateToken(user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        // ✅ Lưu refresh token vào database
        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));
        userService.save(user);

        AuthResponse response = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();

        return ResponseEntity.ok(response);
    }

    // ✅ THÊM API MỚI: Làm mới access token
    @Operation(summary = "Làm mới access token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Làm mới token thành công"),
            @ApiResponse(responseCode = "403", description = "Refresh token không hợp lệ")
    })
    @PostMapping("/refresh")
    @Transactional
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (refreshToken == null || refreshToken.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Refresh token không được để trống");
            return ResponseEntity.badRequest().body(error);
        }

        // Tìm user theo refresh token
        User user = userService.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token không hợp lệ"));

        // Kiểm tra refresh token có hết hạn không
        if (jwtUtil.isRefreshTokenExpired(user.getRefreshTokenExpiry())) {
            // Xóa refresh token đã hết hạn
            user.setRefreshToken(null);
            user.setRefreshTokenExpiry(null);
            userService.save(user);

            Map<String, String> error = new HashMap<>();
            error.put("message", "Refresh token đã hết hạn. Vui lòng đăng nhập lại");
            return ResponseEntity.status(403).body(error);
        }

        // Tạo access token mới
        String newAccessToken = jwtUtil.generateToken(user.getEmail());

        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", newAccessToken);
        response.put("refreshToken", refreshToken);
        response.put("tokenType", "Bearer");

        return ResponseEntity.ok(response);
    }

    // ✅ THÊM API MỚI: Đăng xuất
    @Operation(summary = "Đăng xuất")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Đăng xuất thành công")
    })
    @PostMapping("/logout")
    @Transactional
    public ResponseEntity<?> logout(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (refreshToken != null && !refreshToken.isEmpty()) {
            userService.findByRefreshToken(refreshToken).ifPresent(user -> {
                user.setRefreshToken(null);
                user.setRefreshTokenExpiry(null);
                userService.save(user);
            });
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "Đăng xuất thành công");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test")
    public ResponseEntity<?> test() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "API hoạt động OK!");
        return ResponseEntity.ok(response);
    }
}