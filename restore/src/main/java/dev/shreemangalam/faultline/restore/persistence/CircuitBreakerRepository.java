package dev.shreemangalam.faultline.restore.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CircuitBreakerRepository extends JpaRepository<CircuitBreakerEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT circuit FROM CircuitBreakerEntity circuit WHERE circuit.targetSystem = :targetSystem")
    Optional<CircuitBreakerEntity> lockByTargetSystem(@Param("targetSystem") String targetSystem);
}