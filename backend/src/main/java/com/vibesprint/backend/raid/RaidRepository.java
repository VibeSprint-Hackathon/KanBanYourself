package com.vibesprint.backend.raid;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface RaidRepository extends JpaRepository<Raid, Long> {

    Optional<Raid> findFirstByOrderByIdAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select raid from Raid raid where raid.id = (select min(candidate.id) from Raid candidate)")
    Optional<Raid> findFirstForUpdate();
}
