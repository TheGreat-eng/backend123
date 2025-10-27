package com.example.iotserver.repository;

import com.example.iotserver.entity.Rule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RuleRepository extends JpaRepository<Rule, Long> {

    // Tìm quy tắc theo Farm
    List<Rule> findByFarmId(Long farmId);

    // Tìm quy tắc đang kích hoạt
    List<Rule> findByFarmIdAndEnabled(Long farmId, Boolean enabled);

    // Tìm tất cả quy tắc đang kích hoạt (để chạy tự động)
    @Query("SELECT r FROM Rule r WHERE r.enabled = true ORDER BY r.priority DESC")
    List<Rule> findAllEnabledRules();

    // Tìm quy tắc theo Farm và enabled, sắp xếp theo priority
    @Query("SELECT r FROM Rule r WHERE r.farm.id = :farmId AND r.enabled = true ORDER BY r.priority DESC")
    List<Rule> findEnabledRulesByFarmOrderByPriority(Long farmId);

    // Kiểm tra tồn tại
    boolean existsByIdAndFarmId(Long id, Long farmId);

    // Đếm số quy tắc của Farm
    long countByFarmId(Long farmId);

    // Đếm số quy tắc đang kích hoạt
    long countByFarmIdAndEnabled(Long farmId, Boolean enabled);
}