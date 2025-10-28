package com.example.iotserver.repository;

import com.example.iotserver.entity.Device;
import com.example.iotserver.enums.DeviceStatus; // Thêm import
import com.example.iotserver.enums.DeviceType; // Thêm import
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByDeviceId(String deviceId);

    // ✅ THÊM METHOD NÀY
    @Query("SELECT d FROM Device d " +
            "LEFT JOIN FETCH d.farm f " +
            "LEFT JOIN FETCH f.owner " +
            "WHERE d.deviceId = :deviceId")
    Optional<Device> findByDeviceIdWithFarmAndOwner(@Param("deviceId") String deviceId);

    List<Device> findByFarmId(Long farmId);

    List<Device> findByFarmIdAndType(Long farmId, DeviceType type);

    List<Device> findByFarmIdAndStatus(Long farmId, DeviceStatus status);

    @Query("SELECT d FROM Device d WHERE d.farm.id = :farmId AND d.status = 'ONLINE'")
    List<Device> findOnlineDevicesByFarmId(Long farmId);

    @Query("SELECT d FROM Device d WHERE d.lastSeen < :threshold")
    List<Device> findStaleDevices(LocalDateTime threshold);

    boolean existsByDeviceId(String deviceId);

    long countByFarmId(Long farmId);

    long countByFarmIdAndStatus(Long farmId, DeviceStatus status);

    // DeviceRepository.java
    @Modifying
    @Transactional
    void deleteByFarmId(Long farmId);

    // THÊM PHƯƠNG THỨC NÀY
    long countByStatus(DeviceStatus status); // Sửa nốt phương thức này

    @Query("SELECT d FROM Device d WHERE d.farm.id IN :farmIds AND (LOWER(d.name) LIKE :keyword OR LOWER(d.deviceId) LIKE :keyword)")
    List<Device> searchDevicesInFarms(@Param("farmIds") List<Long> farmIds, @Param("keyword") String keyword);

}