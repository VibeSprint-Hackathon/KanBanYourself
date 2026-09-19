package com.vibesprint.backend.raid;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;

public interface RaidRepository extends JpaRepository<Raid, Long> {

    List<Raid> findAllByOrderByIdDesc();

    Optional<Raid> findByStatus(RaidStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select raid from Raid raid where raid.status = com.vibesprint.backend.raid.RaidStatus.ACTIVE")
    Optional<Raid> findActiveForUpdate();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select raid from Raid raid where raid.id = :id")
    Optional<Raid> findByIdForUpdate(long id);
}
