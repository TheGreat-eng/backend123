package com.example.iotserver.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.iotserver.enums.UserRole;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("deleted = false")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password; // Hashed password

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false) // <-- SỬA LẠI TÊN CỘT (tùy chọn)
    private UserRole role = UserRole.FARMER; // <-- THAY ĐỔI Ở ĐÂY

    @Column(nullable = false)
    private Boolean enabled = true;

    // VVVV--- THÊM TRƯỜNG MỚI DƯỚI ĐÂY ---VVVV
    @Column(nullable = false)
    private Boolean deleted = false;

    // ✅ THÊM CÁC TRƯỜNG MỚI CHO REFRESH TOKEN
    @Column(name = "refresh_token", unique = true, length = 500)
    private String refreshToken;

    @Column(name = "refresh_token_expiry")
    private LocalDateTime refreshTokenExpiry;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    @JsonManagedReference // ✅ Parent side
    private List<Farm> farms = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}