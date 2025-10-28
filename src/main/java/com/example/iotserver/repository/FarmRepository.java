package com.example.iotserver.repository;

import com.example.iotserver.entity.Farm;
import com.example.iotserver.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // <-- thêm import này
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FarmRepository extends JpaRepository<Farm, Long> {

    // Lấy theo ownerId (cột owner_id)
    List<Farm> findByOwnerId(Long ownerId);

    Optional<Farm> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByIdAndOwnerId(Long id, Long ownerId);

    long countByOwnerId(Long ownerId);

    // Lấy theo entity User (đúng với field 'owner' trong Farm)
    List<Farm> findByOwner(User owner);

    // Nếu chỉ cần farm do chính owner sở hữu:
    @Query("SELECT f FROM Farm f WHERE f.owner.id = :userId")
    List<Farm> findFarmsByUserAccess(@Param("userId") Long userId);

    // Nếu sau này bạn có bảng thành viên (FarmMember) và muốn lấy cả farm được
    // share,
    // dùng JPQL dưới đây và bật lại khi có entity liên quan:
    // @Query("SELECT DISTINCT f FROM Farm f LEFT JOIN FarmMember fm ON fm.farm = f
    // " +
    // "WHERE f.owner.id = :userId OR fm.user.id = :userId")
    // List<Farm> findFarmsByUserAccess(@Param("userId") Long userId);
}
