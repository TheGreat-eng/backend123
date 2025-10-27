package com.example.iotserver.repository;

import com.example.iotserver.entity.Farm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FarmRepository extends JpaRepository<Farm, Long> {

    List<Farm> findByOwnerId(Long ownerId);

    Optional<Farm> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByIdAndOwnerId(Long id, Long ownerId);

    // Comment hoặc xóa method này nếu chưa cần
    // @Query("SELECT f FROM Farm f WHERE f.owner.id = :userId OR f.id IN " +
    // "(SELECT fm.farm.id FROM FarmMember fm WHERE fm.user.id = :userId)")
    // List<Farm> findFarmsByUserAccess(Long userId);

    // Thay bằng query đơn giản hơn (chỉ lấy farm của owner)
    @Query("SELECT f FROM Farm f WHERE f.owner.id = :userId")
    List<Farm> findFarmsByUserAccess(Long userId);

    long countByOwnerId(Long ownerId);
}
