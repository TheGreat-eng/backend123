package com.example.iotserver.repository;

import com.example.iotserver.entity.RuleExecutionLog;
import com.example.iotserver.entity.RuleExecutionLog.ExecutionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RuleExecutionLogRepository extends JpaRepository<RuleExecutionLog, Long> {

    // Tìm log theo quy tắc
    Page<RuleExecutionLog> findByRuleId(Long ruleId, Pageable pageable);

    // Tìm log theo khoảng thời gian
    List<RuleExecutionLog> findByExecutedAtBetween(LocalDateTime start, LocalDateTime end);

    // Tìm log thành công
    List<RuleExecutionLog> findByRuleIdAndStatus(Long ruleId, ExecutionStatus status);

    // Tìm log gần nhất của quy tắc
    @Query("SELECT l FROM RuleExecutionLog l WHERE l.rule.id = :ruleId ORDER BY l.executedAt DESC")
    List<RuleExecutionLog> findLatestByRuleId(Long ruleId, Pageable pageable);

    // Đếm số lần thực thi thành công
    long countByRuleIdAndStatus(Long ruleId, ExecutionStatus status);

    // Xóa log cũ (tự động dọn dẹp)
    @Query("DELETE FROM RuleExecutionLog l WHERE l.executedAt < :threshold")
    void deleteOldLogs(LocalDateTime threshold);

    // ====> THÊM METHOD MỚI NÀY <====
    @Modifying // Bắt buộc phải có khi thực hiện query Cập nhật hoặc Xóa
    @Query("DELETE FROM RuleExecutionLog l WHERE l.rule.id = :ruleId")
    void deleteByRuleId(Long ruleId);
}