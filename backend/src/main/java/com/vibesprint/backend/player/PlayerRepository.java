package com.vibesprint.backend.player;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    Optional<Player> findByName(String name);

    List<Player> findAllByOrderByIdAsc();

    Optional<Player> findByGithubUserId(Long githubUserId);

    Optional<Player> findByGithubLoginIgnoreCase(String githubLogin);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select player from Player player where player.id = :id")
    Optional<Player> findByIdForUpdate(@Param("id") Long id);
}
