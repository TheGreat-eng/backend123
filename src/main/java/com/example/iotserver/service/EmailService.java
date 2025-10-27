package com.example.iotserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender emailSender;

    @Async // Chạy bất đồng bộ để không làm chậm request chính
    public void sendSimpleMessage(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@smartfarm.com"); // Có thể đặt cố định
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            emailSender.send(message);
            log.info("✅ Đã gửi email thành công tới {}", to);
        } catch (Exception e) {
            log.error("❌ Lỗi khi gửi email tới {}: {}", to, e.getMessage());
        }
    }
}