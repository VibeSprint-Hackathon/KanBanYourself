package com.vibesprint.backend.achievement;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class PlayerAchievementId implements Serializable {

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "achievement_key", nullable = false, length = 64)
    private String achievementKey;

    protected PlayerAchievementId() {
    }

    PlayerAchievementId(long playerId, AchievementKey achievementKey) {
        this.playerId = playerId;
        this.achievementKey = achievementKey.name();
    }

    AchievementKey achievementKey() {
        return AchievementKey.valueOf(achievementKey);
    }

    @Override
    public boolean equals(Object value) {
        if (this == value) return true;
        if (!(value instanceof PlayerAchievementId other)) return false;
        return Objects.equals(playerId, other.playerId) && Objects.equals(achievementKey, other.achievementKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, achievementKey);
    }
}
