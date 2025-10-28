package com.example.iotserver.service;

import com.example.iotserver.entity.User;
import com.example.iotserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;

    /**
     * Lấy thông tin User entity từ database của người dùng đang được xác thực.
     * Phương thức này kết nối với Spring Security Context để tìm ra ai đang đăng
     * nhập.
     *
     * @return Đối tượng User của người dùng hiện tại.
     * @throws UsernameNotFoundException nếu không tìm thấy người dùng trong DB hoặc
     *                                   người dùng chưa đăng nhập.
     */
    public User getCurrentAuthenticatedUser() {
        // Lấy thông tin xác thực từ Spring Security's context
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String username;

        // "Principal" có thể là một đối tượng UserDetails hoặc một String (tùy cấu
        // hình)
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }

        // Dùng username (chính là email) để truy vấn trong database và lấy ra toàn bộ
        // thông tin User
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + username));
    }
}