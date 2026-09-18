package com.vibesprint.backend.raid;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RaidRepository extends JpaRepository<Raid, Long> {

    Optional<Raid> findFirstByOrderByIdAsc();
}
