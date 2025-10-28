package com.example.iotserver.dto;

import com.example.iotserver.enums.UserRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDTO {
    private Long id;
    private String email;
    private String username;
    private String fullName;
    private String phoneNumber;
    private UserRole role;
}