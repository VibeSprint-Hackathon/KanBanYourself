package com.vibesprint.backend.quest;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuestRepository extends JpaRepository<Quest, Long> {

    @Query("""
            select quest from Quest quest
            order by case quest.status
                when com.vibesprint.backend.quest.QuestStatus.BACKLOG then 0
                when com.vibesprint.backend.quest.QuestStatus.TODO then 1
                when com.vibesprint.backend.quest.QuestStatus.IN_PROGRESS then 2
                when com.vibesprint.backend.quest.QuestStatus.TESTING then 3
                when com.vibesprint.backend.quest.QuestStatus.DONE then 4
            end, quest.sortOrder, quest.id
            """)
    List<Quest> findAllInBoardOrder();

    List<Quest> findAllByStatusOrderBySortOrderAscIdAsc(QuestStatus status);

    Optional<Quest> findByExternalReference(String externalReference);

    boolean existsByStatus(QuestStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select quest from Quest quest where quest.id = :id")
    Optional<Quest> findByIdForUpdate(@Param("id") Long id);
}
