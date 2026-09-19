package com.vibesprint.backend.achievement;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "player_achievement")
public class PlayerAchievement {

    @EmbeddedId
    private PlayerAchievementId id;

    @Column(name = "progress", nullable = false)
    private int progress;

    @Column(name = "unlocked_at")
    private Instant unlockedAt;

    protected PlayerAchievement() {
    }

    PlayerAchievement(long playerId, AchievementKey key) {
        this.id = new PlayerAchievementId(playerId, key);
    }

    public int getProgress() {
        return progress;
    }

    public AchievementKey getAchievementKey() {
        return id.achievementKey();
    }

    public Instant getUnlockedAt() {
        return unlockedAt;
    }

    boolean increment(int amount, int target, Instant now) {
        if (amount <= 0 || unlockedAt != null) return false;
        progress = Math.min(target, Math.addExact(progress, amount));
        if (progress >= target) {
            unlockedAt = now;
            return true;
        }
        return false;
    }
}
