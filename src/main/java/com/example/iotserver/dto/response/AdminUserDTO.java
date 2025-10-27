// src/main/java/com/example/iotserver/dto/response/AdminUserDTO.java

package com.example.iotserver.dto.response;

import com.example.iotserver.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminUserDTO {
    private Long id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private UserRole role;
    private Boolean enabled; // Trạng thái tài khoản (true: hoạt động, false: bị khóa)
    private Boolean deleted; // <-- THÊM TRƯỜNG NÀY

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLogin;
}