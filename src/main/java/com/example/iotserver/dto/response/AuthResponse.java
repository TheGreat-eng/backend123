package com.example.iotserver.dto.response;

import com.example.iotserver.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken; // ✅ Đổi tên từ "token"
    private String refreshToken; // ✅ THÊM MỚI
    private String tokenType; // ✅ THÊM MỚI
    private Long userId;
    private String email;
    private String fullName;
    private UserRole role;
}