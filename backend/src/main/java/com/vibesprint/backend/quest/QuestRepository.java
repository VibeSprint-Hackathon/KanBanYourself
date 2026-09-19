package com.vibesprint.backend.quest;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuestRepository extends JpaRepository<Quest, Long> {

    List<Quest> findAllByOrderByIdAsc();

    Optional<Quest> findByExternalReference(String externalReference);

    boolean existsByStatus(QuestStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select quest from Quest quest where quest.id = :id")
    Optional<Quest> findByIdForUpdate(@Param("id") Long id);
}
