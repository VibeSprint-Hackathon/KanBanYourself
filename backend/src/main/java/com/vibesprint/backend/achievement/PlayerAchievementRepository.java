package com.vibesprint.backend.achievement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerAchievementRepository extends JpaRepository<PlayerAchievement, PlayerAchievementId> {
    List<PlayerAchievement> findAllByIdPlayerId(long playerId);
}
