package com.example.iotserver.service.impl;

import com.example.iotserver.dto.request.SetPasswordRequest;
import com.example.iotserver.dto.request.UpdateUserRequest;
import com.example.iotserver.entity.User;
import com.example.iotserver.exception.ResourceNotFoundException;
import com.example.iotserver.repository.UserRepository;
import com.example.iotserver.service.UserService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    // --- TRIỂN KHAI CÁC PHƯƠNG THỨC MỚI ---

    @Override
    public Page<User> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public User lockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setEnabled(false); // Khóa tài khoản
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User unlockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setEnabled(true); // Mở khóa tài khoản
        return userRepository.save(user);
    }

    // ✅ THÊM IMPLEMENTATION MỚI
    @Override
    public Optional<User> findByRefreshToken(String refreshToken) {
        return userRepository.findByRefreshToken(refreshToken);
    }

    // VVVV--- TRIỂN KHAI CÁC PHƯƠNG THỨC MỚI ---VVVV

    @Override
    public Optional<User> findUserByIdEvenIfDeleted(Long userId) {
        // Dùng findById vì nó không bị ảnh hưởng bởi @Where trong một số trường hợp,
        // hoặc bạn có thể tạo query riêng trong repository nếu cần
        return userRepository.findById(userId);
    }

    @Override
    @Transactional
    public User updateUserAsAdmin(Long userId, UpdateUserRequest request) {
        User user = findUserByIdEvenIfDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User setPasswordAsAdmin(Long userId, SetPasswordRequest request) {
        User user = findUserByIdEvenIfDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void softDeleteUser(Long userId) {
        User user = findUserByIdEvenIfDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setDeleted(true);
        user.setEnabled(false); // Một user bị xóa cũng nên bị vô hiệu hóa

        userRepository.save(user);
    }
}