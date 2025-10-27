package com.example.iotserver.repository;

import com.example.iotserver.entity.PlantHealthAlert;
import com.example.iotserver.entity.PlantHealthAlert.AlertType;
import com.example.iotserver.entity.PlantHealthAlert.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PlantHealthAlertRepository extends JpaRepository<PlantHealthAlert, Long> {

    // 1. Tìm cảnh báo chưa xử lý
    List<PlantHealthAlert> findByFarmIdAndResolvedFalseOrderByDetectedAtDesc(Long farmId);

    // 2. Tìm theo khoảng thời gian
    List<PlantHealthAlert> findByFarmIdAndDetectedAtBetweenOrderByDetectedAtDesc(
            Long farmId, LocalDateTime startDate, LocalDateTime endDate);

    // 3. Tìm theo mức độ nghiêm trọng
    List<PlantHealthAlert> findByFarmIdAndSeverityAndResolvedFalse(
            Long farmId, Severity severity);

    // 4. Tìm theo loại cảnh báo
    List<PlantHealthAlert> findByFarmIdAndAlertTypeAndResolvedFalse(
            Long farmId, AlertType alertType);

    // 5. Đếm số cảnh báo theo mức độ
    @Query("SELECT COUNT(a) FROM PlantHealthAlert a " +
            "WHERE a.farmId = :farmId " +
            "AND a.severity = :severity " +
            "AND a.resolved = false")
    long countUnresolvedBySeverity(
            @Param("farmId") Long farmId,
            @Param("severity") Severity severity);

    // 6. Lấy cảnh báo mới nhất
    PlantHealthAlert findTopByFarmIdOrderByDetectedAtDesc(Long farmId);

    // 7. Xóa cảnh báo cũ
    void deleteByResolvedTrueAndResolvedAtBefore(LocalDateTime date);
}