package com.example.iotserver.service;

import com.example.iotserver.dto.FarmDTO;
import com.example.iotserver.entity.Farm;
import com.example.iotserver.entity.Rule;
import com.example.iotserver.entity.User;
import com.example.iotserver.enums.DeviceStatus;
import com.example.iotserver.repository.DeviceRepository;
import com.example.iotserver.repository.FarmRepository;
import com.example.iotserver.repository.RuleRepository;
import com.example.iotserver.repository.UserRepository;
import com.example.iotserver.repository.WeatherRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable; // <-- THÊM IMPORT
import org.springframework.cache.annotation.CacheEvict; // <-- THÊM IMPORT

@Service
@Slf4j
@RequiredArgsConstructor
public class FarmService {

    private final FarmRepository farmRepository;
    private final UserRepository userRepository;
    private final WeatherRepository weatherRepository;
    private final RuleRepository ruleRepository;
    private final DeviceRepository deviceRepository;
    private final RuleService ruleService; // Dùng lại logic xóa Rule phức tạp

    @Transactional
    public FarmDTO createFarm(Long userId, FarmDTO dto) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Farm farm = new Farm();
        farm.setName(dto.getName());
        farm.setDescription(dto.getDescription());
        farm.setLocation(dto.getLocation());
        farm.setArea(dto.getArea());
        farm.setOwner(owner);

        Farm saved = farmRepository.save(farm);
        log.info("Created farm: {} for user: {}", saved.getId(), userId);

        return mapToDTO(saved);
    }

    @Transactional
    @CacheEvict(value = "farms", key = "#farmId") // <-- THÊM ANNOTATION NÀY
    public FarmDTO updateFarm(Long farmId, Long userId, FarmDTO dto) {
        Farm farm = farmRepository.findByIdAndOwnerId(farmId, userId)
                .orElseThrow(() -> new RuntimeException("Farm not found or access denied"));

        if (dto.getName() != null)
            farm.setName(dto.getName());
        if (dto.getDescription() != null)
            farm.setDescription(dto.getDescription());
        if (dto.getLocation() != null)
            farm.setLocation(dto.getLocation());
        if (dto.getArea() != null)
            farm.setArea(dto.getArea());

        Farm updated = farmRepository.save(farm);
        log.info("Updated farm: {}", updated.getId());

        return mapToDTO(updated);
    }

    @Transactional
    @CacheEvict(value = "farms", key = "#farmId") // <-- THÊM ANNOTATION NÀY
    public void deleteFarm(Long farmId, Long userId) {
        Farm farm = farmRepository.findByIdAndOwnerId(farmId, userId)
                .orElseThrow(() -> new RuntimeException("Farm not found or access denied"));

        // ====> THÊM LOGIC XÓA CÁC BẢN GHI CON <====

        // 1. Xóa dữ liệu thời tiết
        weatherRepository.deleteByFarmId(farmId); // Cần thêm method này vào WeatherRepository

        // 2. Xóa tất cả các quy tắc thuộc farm
        List<Rule> rulesToDelete = ruleRepository.findByFarmId(farmId);
        for (Rule rule : rulesToDelete) {
            ruleService.deleteRule(rule.getId()); // Tái sử dụng logic xóa rule đã có (xóa cả log)
        }

        // 3. Xóa tất cả các thiết bị thuộc farm
        deviceRepository.deleteByFarmId(farmId); // Cần thêm method này vào DeviceRepository

        // 4. Cuối cùng, xóa nông trại
        farmRepository.delete(farm);

        log.info("Đã xóa nông trại {} và tất cả dữ liệu liên quan", farmId);
    }

    @Cacheable(value = "farms", key = "#farmId") // <-- THÊM ANNOTATION NÀY
    public FarmDTO getFarm(Long farmId, Long userId) {
        log.info("DATABASE HIT: Lấy thông tin farm với ID: {}", farmId); // Thêm log để kiểm tra
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new RuntimeException("Farm not found"));

        // Check access permission
        if (!farm.getOwner().getId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        return mapToDTO(farm);
    }

    public List<FarmDTO> getUserFarms(Long userId) {
        return farmRepository.findByOwnerId(userId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<FarmDTO> getFarmsWithAccess(Long userId) {
        // Tạm thời chỉ trả về farms của owner
        // Sau này có thể mở rộng thêm logic member
        return farmRepository.findByOwnerId(userId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private FarmDTO mapToDTO(Farm farm) {

        // VVVV--- THÊM 2 DÒNG TRUY VẤN NÀY ---VVVV
        long totalDevices = deviceRepository.countByFarmId(farm.getId());
        long onlineDevices = deviceRepository.countByFarmIdAndStatus(farm.getId(), DeviceStatus.ONLINE);
        // ^^^^----------------------------------^^^^

        FarmDTO dto = new FarmDTO();
        dto.setId(farm.getId());
        dto.setName(farm.getName());
        dto.setDescription(farm.getDescription());
        dto.setLocation(farm.getLocation());
        dto.setArea(farm.getArea());
        dto.setOwnerId(farm.getOwner().getId());
        dto.setOwnerName(farm.getOwner().getFullName());
        dto.setCreatedAt(farm.getCreatedAt());
        dto.setUpdatedAt(farm.getUpdatedAt());

        // VVVV--- GÁN CÁC GIÁ TRỊ VỪA ĐẾM ĐƯỢC VÀO DTO ---VVVV
        dto.setTotalDevices(totalDevices);
        dto.setOnlineDevices(onlineDevices);
        // ^^^^---------------------------------------------^^^^

        return dto;
    }
}
