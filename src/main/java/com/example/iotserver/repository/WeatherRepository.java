package com.example.iotserver.repository;

import com.example.iotserver.entity.Weather;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeatherRepository extends JpaRepository<Weather, Long> {

    // Lấy weather mới nhất của farm
    Optional<Weather> findTopByFarmIdOrderByRecordedAtDesc(Long farmId);

    // Lấy weather trong khoảng thời gian
    List<Weather> findByFarmIdAndRecordedAtBetweenOrderByRecordedAtDesc(
            Long farmId, LocalDateTime start, LocalDateTime end);

    // Xóa dữ liệu cũ (> 7 ngày)
    @Query("DELETE FROM Weather w WHERE w.recordedAt < :threshold")
    void deleteOldWeatherData(LocalDateTime threshold);

    @Modifying
    @Transactional
    void deleteByFarmId(Long farmId);
}