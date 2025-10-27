package com.example.iotserver.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "rule_execution_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Quy tắc nào được thực thi
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private Rule rule;

    // Thời gian thực thi
    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    // Kết quả thực thi
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status;

    // Điều kiện có đúng không?
    @Column(name = "conditions_met")
    private Boolean conditionsMet;

    // Chi tiết điều kiện (JSON string)
    @Column(columnDefinition = "TEXT")
    private String conditionDetails;

    // Hành động đã thực hiện (JSON string)
    @Column(columnDefinition = "TEXT")
    private String actionsPerformed;

    // Thông báo lỗi (nếu có)
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    // Thời gian thực thi (ms)
    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    public enum ExecutionStatus {
        SUCCESS, // Thành công
        FAILED, // Thất bại
        SKIPPED, // Bỏ qua (điều kiện không đúng)
        PARTIAL // Một số hành động thành công
    }
}