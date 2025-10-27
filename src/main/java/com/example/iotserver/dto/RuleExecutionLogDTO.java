package com.example.iotserver.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RuleExecutionLogDTO {

    private Long id;
    private Long ruleId;
    private String ruleName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime executedAt;

    private String status; // SUCCESS, FAILED, SKIPPED
    private Boolean conditionsMet; // Điều kiện có đúng không?
    private String conditionDetails; // Chi tiết điều kiện (JSON)
    private String actionsPerformed; // Hành động đã thực hiện (JSON)
    private String errorMessage; // Lỗi (nếu có)
    private Long executionTimeMs; // Thời gian thực thi (ms)
}