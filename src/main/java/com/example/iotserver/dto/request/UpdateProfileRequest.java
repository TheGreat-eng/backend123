package com.example.iotserver.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    // Người dùng chỉ được phép cập nhật các trường này
    // Email và Username không cho phép thay đổi qua API này để tránh phức tạp

    @Size(min = 3, max = 100, message = "Họ tên phải từ 3-100 ký tự")
    private String fullName;

    @Size(min = 10, max = 15, message = "Số điện thoại phải từ 10-15 ký tự")
    private String phoneNumber;
}