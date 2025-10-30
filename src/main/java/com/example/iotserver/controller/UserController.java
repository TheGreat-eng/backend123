package com.example.iotserver.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.iotserver.dto.UserDTO;
import com.example.iotserver.dto.request.ChangePasswordRequest;
import com.example.iotserver.dto.request.UpdateProfileRequest;
import com.example.iotserver.dto.response.ApiResponse;
import com.example.iotserver.entity.User;
import com.example.iotserver.service.AuthenticationService;
import com.example.iotserver.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "10. User", description = "API quản lý người dùng")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationService authenticationService; // <-- Thêm service này

    // (Có thể thêm các API khác để lấy/cập nhật thông tin user ở đây)

    // VVVV--- API MỚI: LẤY THÔNG TIIN CÁ NHÂN ---VVVV
    @GetMapping("/me")
    @Operation(summary = "Lấy thông tin của người dùng hiện tại")
    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUserProfile() {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        UserDTO userDTO = UserDTO.builder()
                .id(currentUser.getId())
                .email(currentUser.getEmail())
                .fullName(currentUser.getFullName())
                .phoneNumber(currentUser.getPhoneNumber())
                .role(currentUser.getRole())
                .build();
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin người dùng thành công", userDTO));
    }

    // VVVV--- API MỚI: CẬP NHẬT THÔNG TIN CÁ NHÂN ---VVVV
    @PutMapping("/me")
    @Operation(summary = "Cập nhật thông tin của người dùng hiện tại")
    public ResponseEntity<ApiResponse<UserDTO>> updateCurrentUserProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();

        // Cập nhật các trường được phép
        currentUser.setFullName(request.getFullName());
        currentUser.setPhoneNumber(request.getPhoneNumber());

        User updatedUser = userService.save(currentUser); // Lưu lại thay đổi

        UserDTO userDTO = UserDTO.builder()
                .id(updatedUser.getId())
                .email(updatedUser.getEmail())
                .fullName(updatedUser.getFullName())
                .phoneNumber(updatedUser.getPhoneNumber())
                .role(updatedUser.getRole())
                .build();

        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin thành công", userDTO));
    }

    // VVVV--- API ĐỔI MẬT KHẨU (Sửa lại một chút cho nhất quán) ---VVVV
    @PostMapping("/change-password")
    @Operation(summary = "Thay đổi mật khẩu của người dùng hiện tại")
    public ResponseEntity<ApiResponse<String>> changePassword(@RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        // authenticationService đã có sẵn logic này, ta có thể dùng nó
        User user = authenticationService.getCurrentAuthenticatedUser();

        // 1. Kiểm tra mật khẩu cũ có đúng không
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            // Thay vì trả về Map, dùng ApiResponse cho nhất quán
            return ResponseEntity.badRequest().body(ApiResponse.error("Mật khẩu cũ không chính xác."));
        }

        // 2. Cập nhật mật khẩu mới
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userService.save(user);

        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công.", "success"));
    }
}
