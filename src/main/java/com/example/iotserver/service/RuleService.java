package com.example.iotserver.service;

import com.example.iotserver.dto.RuleDTO;
import com.example.iotserver.dto.RuleExecutionLogDTO;
import com.example.iotserver.entity.Farm;
import com.example.iotserver.entity.Rule;
import com.example.iotserver.entity.RuleCondition;
import com.example.iotserver.entity.RuleExecutionLog;
import com.example.iotserver.repository.FarmRepository;
import com.example.iotserver.repository.RuleExecutionLogRepository;
import com.example.iotserver.repository.RuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RuleService {

    private final RuleRepository ruleRepository;
    private final FarmRepository farmRepository;
    private final RuleExecutionLogRepository logRepository;

    /**
     * Tạo quy tắc mới
     */
    @Transactional
    public RuleDTO createRule(Long farmId, RuleDTO dto) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nông trại"));

        // Tạo Rule entity
        Rule rule = Rule.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .farm(farm)
                .enabled(dto.getEnabled() != null ? dto.getEnabled() : true)
                .priority(dto.getPriority() != null ? dto.getPriority() : 0)
                .executionCount(0L)
                .build();

        // Thêm điều kiện
        if (dto.getConditions() != null) {
            for (RuleDTO.ConditionDTO condDto : dto.getConditions()) {
                RuleCondition condition = RuleCondition.builder()
                        .rule(rule)
                        .type(RuleCondition.ConditionType.valueOf(condDto.getType()))
                        .field(condDto.getField())
                        .operator(RuleCondition.Operator.valueOf(condDto.getOperator()))
                        .value(condDto.getValue())
                        .deviceId(condDto.getDeviceId())
                        .logicalOperator(condDto.getLogicalOperator() != null
                                ? RuleCondition.LogicalOperator.valueOf(condDto.getLogicalOperator())
                                : RuleCondition.LogicalOperator.AND)
                        .orderIndex(condDto.getOrderIndex() != null ? condDto.getOrderIndex() : 0)
                        .build();
                rule.getConditions().add(condition);
            }
        }

        // Thêm hành động
        if (dto.getActions() != null) {
            for (RuleDTO.ActionDTO actDto : dto.getActions()) {
                Rule.RuleAction action = Rule.RuleAction.builder()
                        .type(Rule.ActionType.valueOf(actDto.getType()))
                        .deviceId(actDto.getDeviceId())
                        .durationSeconds(actDto.getDurationSeconds())
                        .message(actDto.getMessage())
                        .build();
                rule.getActions().add(action);
            }
        }

        Rule saved = ruleRepository.save(rule);
        log.info("Đã tạo quy tắc: {} cho nông trại: {}", saved.getName(), farmId);

        return mapToDTO(saved);
    }

    /**
     * Cập nhật quy tắc
     */
    @Transactional
    public RuleDTO updateRule(Long ruleId, RuleDTO dto) {
        Rule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy quy tắc"));

        // Cập nhật thông tin cơ bản
        if (dto.getName() != null)
            rule.setName(dto.getName());
        if (dto.getDescription() != null)
            rule.setDescription(dto.getDescription());
        if (dto.getEnabled() != null)
            rule.setEnabled(dto.getEnabled());
        if (dto.getPriority() != null)
            rule.setPriority(dto.getPriority());

        // Cập nhật điều kiện (xóa cũ, thêm mới)
        if (dto.getConditions() != null) {
            rule.getConditions().clear();
            for (RuleDTO.ConditionDTO condDto : dto.getConditions()) {
                RuleCondition condition = RuleCondition.builder()
                        .rule(rule)
                        .type(RuleCondition.ConditionType.valueOf(condDto.getType()))
                        .field(condDto.getField())
                        .operator(RuleCondition.Operator.valueOf(condDto.getOperator()))
                        .value(condDto.getValue())
                        .deviceId(condDto.getDeviceId())
                        .logicalOperator(condDto.getLogicalOperator() != null
                                ? RuleCondition.LogicalOperator.valueOf(condDto.getLogicalOperator())
                                : RuleCondition.LogicalOperator.AND)
                        .orderIndex(condDto.getOrderIndex() != null ? condDto.getOrderIndex() : 0)
                        .build();
                rule.getConditions().add(condition);
            }
        }

        // Cập nhật hành động
        if (dto.getActions() != null) {
            rule.getActions().clear();
            for (RuleDTO.ActionDTO actDto : dto.getActions()) {
                Rule.RuleAction action = Rule.RuleAction.builder()
                        .type(Rule.ActionType.valueOf(actDto.getType()))
                        .deviceId(actDto.getDeviceId())
                        .durationSeconds(actDto.getDurationSeconds())
                        .message(actDto.getMessage())
                        .build();
                rule.getActions().add(action);
            }
        }

        Rule updated = ruleRepository.save(rule);
        log.info("Đã cập nhật quy tắc: {}", updated.getName());

        return mapToDTO(updated);
    }

    /**
     * Xóa quy tắc
     */
    @Transactional // Đảm bảo toàn bộ thao tác là một transaction
    public void deleteRule(Long ruleId) {
        Rule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy quy tắc"));

        // ====> THÊM LOGIC XÓA CÁC BẢN GHI CON TRƯỚC <====

        // 1. Tìm tất cả các log thuộc về quy tắc này
        // (Cách này không hiệu quả nếu có nhiều log, xem cách tối ưu hơn bên dưới)
        // List<RuleExecutionLog> logsToDelete = logRepository.findByRuleId(ruleId,
        // Pageable.unpaged()).getContent();
        // logRepository.deleteAll(logsToDelete);

        // CÁCH TỐI ƯU HƠN: Dùng query xóa trực tiếp (cần tạo method trong Repository)
        logRepository.deleteByRuleId(ruleId);

        // ================================================

        // 2. Sau khi đã xóa hết các bản ghi con, bây giờ mới xóa bản ghi cha
        ruleRepository.delete(rule);

        log.info("Đã xóa quy tắc: {} và các log liên quan", rule.getName());
    }

    /**
     * Lấy chi tiết quy tắc
     */
    public RuleDTO getRule(Long ruleId) {
        Rule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy quy tắc"));
        return mapToDTO(rule);
    }

    /**
     * Lấy danh sách quy tắc của Farm
     */
    public List<RuleDTO> getRulesByFarm(Long farmId) {
        return ruleRepository.findByFarmId(farmId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách quy tắc đang kích hoạt
     */
    public List<RuleDTO> getEnabledRules(Long farmId) {
        return ruleRepository.findByFarmIdAndEnabled(farmId, true)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Bật/tắt quy tắc
     */
    @Transactional
    public RuleDTO toggleRule(Long ruleId, Boolean enabled) {
        Rule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy quy tắc"));

        rule.setEnabled(enabled);
        Rule updated = ruleRepository.save(rule);

        log.info("Đã {} quy tắc: {}", enabled ? "bật" : "tắt", rule.getName());
        return mapToDTO(updated);
    }

    /**
     * Lấy lịch sử thực thi
     */
    public List<RuleExecutionLogDTO> getRuleExecutionLogs(Long ruleId, int limit) {
        List<RuleExecutionLog> logs = logRepository.findLatestByRuleId(
                ruleId,
                PageRequest.of(0, limit));

        return logs.stream()
                .map(this::mapLogToDTO)
                .collect(Collectors.toList());
    }

    // ========== Helper Methods ==========

    private RuleDTO mapToDTO(Rule rule) {
        RuleDTO dto = RuleDTO.builder()
                .id(rule.getId())
                .name(rule.getName())
                .description(rule.getDescription())
                .farmId(rule.getFarm().getId())
                .farmName(rule.getFarm().getName())
                .enabled(rule.getEnabled())
                .priority(rule.getPriority())
                .executionCount(rule.getExecutionCount())
                .lastExecutedAt(rule.getLastExecutedAt())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();

        // Map conditions
        dto.setConditions(rule.getConditions().stream()
                .map(c -> RuleDTO.ConditionDTO.builder()
                        .id(c.getId())
                        .type(c.getType().name())
                        .field(c.getField())
                        .operator(c.getOperator().name())
                        .value(c.getValue())
                        .deviceId(c.getDeviceId())
                        .logicalOperator(c.getLogicalOperator().name())
                        .orderIndex(c.getOrderIndex())
                        .build())
                .collect(Collectors.toList()));

        // Map actions
        dto.setActions(rule.getActions().stream()
                .map(a -> RuleDTO.ActionDTO.builder()
                        .type(a.getType().name())
                        .deviceId(a.getDeviceId())
                        .durationSeconds(a.getDurationSeconds())
                        .message(a.getMessage())
                        .build())
                .collect(Collectors.toList()));

        return dto;
    }

    private RuleExecutionLogDTO mapLogToDTO(RuleExecutionLog log) {
        return RuleExecutionLogDTO.builder()
                .id(log.getId())
                .ruleId(log.getRule().getId())
                .ruleName(log.getRule().getName())
                .executedAt(log.getExecutedAt())
                .status(log.getStatus().name())
                .conditionsMet(log.getConditionsMet())
                .conditionDetails(log.getConditionDetails())
                .actionsPerformed(log.getActionsPerformed())
                .errorMessage(log.getErrorMessage())
                .executionTimeMs(log.getExecutionTimeMs())
                .build();
    }
}