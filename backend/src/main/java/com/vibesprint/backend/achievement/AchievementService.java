package com.vibesprint.backend.achievement;

import com.vibesprint.backend.player.Player;
import com.vibesprint.backend.player.PlayerNotFoundException;
import com.vibesprint.backend.player.PlayerRepository;
import com.vibesprint.backend.progression.ProgressionSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class AchievementService {

    private final PlayerRepository playerRepository;
    private final PlayerAchievementRepository achievementRepository;

    public AchievementService(
            PlayerRepository playerRepository,
            PlayerAchievementRepository achievementRepository
    ) {
        this.playerRepository = playerRepository;
        this.achievementRepository = achievementRepository;
    }

    @Transactional(readOnly = true)
    public AchievementResponse getAchievements(long playerId) {
        if (!playerRepository.existsById(playerId)) {
            throw new PlayerNotFoundException(playerId);
        }

        Map<AchievementKey, PlayerAchievement> states = stateByKey(playerId);
        AchievementKey featuredKey = selectFeatured(states);
        List<AchievementResponse.AchievementView> achievements = AchievementDefinition.CATALOG.stream()
                .map(definition -> toView(definition, states.get(definition.key()), definition.key() == featuredKey))
                .toList();
        int unlocked = (int) achievements.stream().filter(AchievementResponse.AchievementView::unlocked).count();
        return new AchievementResponse(playerId, unlocked, achievements.size(), achievements);
    }

    public List<AchievementUnlock> applyQuestCompletion(
            Player player,
            int xpGained,
            int raidDamage,
            boolean levelUp,
            boolean bossDefeated,
            ProgressionSource source
    ) {
        Map<AchievementKey, PlayerAchievement> states = stateByKey(player.getId());
        List<AchievementUnlock> unlocked = new ArrayList<>();
        Instant now = Instant.now();

        increment(player.getId(), AchievementKey.FIRST_QUEST, 1, states, now, unlocked);
        increment(player.getId(), AchievementKey.QUEST_APPRENTICE, 1, states, now, unlocked);
        increment(player.getId(), AchievementKey.QUEST_MASTER, 1, states, now, unlocked);
        increment(player.getId(), AchievementKey.XP_HUNTER, xpGained, states, now, unlocked);
        if (levelUp) increment(player.getId(), AchievementKey.LEVEL_UP, 1, states, now, unlocked);
        if (raidDamage > 0) {
            increment(player.getId(), AchievementKey.FIRST_STRIKE, 1, states, now, unlocked);
            increment(player.getId(), AchievementKey.HEAVY_HITTER, raidDamage, states, now, unlocked);
            increment(player.getId(), AchievementKey.RAID_CONTRIBUTOR, 1, states, now, unlocked);
        }
        if (bossDefeated && raidDamage > 0) {
            increment(player.getId(), AchievementKey.BOSS_SLAYER, 1, states, now, unlocked);
        }
        if (source == ProgressionSource.GITHUB) {
            increment(player.getId(), AchievementKey.GITHUB_HERO, 1, states, now, unlocked);
        }
        return List.copyOf(unlocked);
    }

    private void increment(
            long playerId,
            AchievementKey key,
            int amount,
            Map<AchievementKey, PlayerAchievement> states,
            Instant now,
            List<AchievementUnlock> unlocked
    ) {
        AchievementDefinition definition = definition(key);
        PlayerAchievement state = states.computeIfAbsent(
                key,
                ignored -> new PlayerAchievement(playerId, key)
        );
        boolean newlyUnlocked = state.increment(amount, definition.targetProgress(), now);
        achievementRepository.save(state);
        if (newlyUnlocked) {
            unlocked.add(new AchievementUnlock(
                    key,
                    definition.name(),
                    definition.description(),
                    definition.category(),
                    definition.rewardLabel()
            ));
        }
    }

    private Map<AchievementKey, PlayerAchievement> stateByKey(long playerId) {
        Map<AchievementKey, PlayerAchievement> states = new EnumMap<>(AchievementKey.class);
        achievementRepository.findAllByIdPlayerId(playerId)
                .forEach(state -> states.put(state.getAchievementKey(), state));
        return states;
    }

    private AchievementKey selectFeatured(Map<AchievementKey, PlayerAchievement> states) {
        AchievementDefinition bestLocked = null;
        double bestProgress = -1;
        for (AchievementDefinition definition : AchievementDefinition.CATALOG) {
            PlayerAchievement state = states.get(definition.key());
            if (state != null && state.getUnlockedAt() != null) continue;
            double progress = state == null ? 0 : (double) state.getProgress() / definition.targetProgress();
            if (progress > bestProgress) {
                bestProgress = progress;
                bestLocked = definition;
            }
        }
        if (bestLocked != null) return bestLocked.key();
        return states.values().stream()
                .filter(state -> state.getUnlockedAt() != null)
                .max((left, right) -> left.getUnlockedAt().compareTo(right.getUnlockedAt()))
                .map(PlayerAchievement::getAchievementKey)
                .orElse(AchievementKey.FIRST_QUEST);
    }

    private AchievementResponse.AchievementView toView(
            AchievementDefinition definition,
            PlayerAchievement state,
            boolean featured
    ) {
        int progress = state == null ? 0 : Math.min(state.getProgress(), definition.targetProgress());
        Instant unlockedAt = state == null ? null : state.getUnlockedAt();
        return new AchievementResponse.AchievementView(
                definition.key(),
                definition.name(),
                definition.description(),
                definition.category(),
                progress,
                definition.targetProgress(),
                unlockedAt != null,
                unlockedAt,
                definition.rewardLabel(),
                featured
        );
    }

    private AchievementDefinition definition(AchievementKey key) {
        return AchievementDefinition.CATALOG.stream()
                .filter(definition -> definition.key() == key)
                .findFirst()
                .orElseThrow();
    }
}
