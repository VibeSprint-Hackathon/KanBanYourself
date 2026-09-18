package com.vibesprint.backend.progression;

import com.vibesprint.backend.player.Player;
import com.vibesprint.backend.player.PlayerRepository;
import com.vibesprint.backend.quest.Quest;
import com.vibesprint.backend.quest.QuestRepository;
import com.vibesprint.backend.quest.QuestStatus;
import com.vibesprint.backend.raid.Raid;
import com.vibesprint.backend.raid.RaidRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgressionService {

    private final QuestRepository questRepository;
    private final PlayerRepository playerRepository;
    private final RaidRepository raidRepository;
    private final ProgressionRules progressionRules;

    public ProgressionService(
            QuestRepository questRepository,
            PlayerRepository playerRepository,
            RaidRepository raidRepository,
            ProgressionRules progressionRules
    ) {
        this.questRepository = questRepository;
        this.playerRepository = playerRepository;
        this.raidRepository = raidRepository;
        this.progressionRules = progressionRules;
    }

    @Transactional
    public ProgressionResult completeQuest(CompleteQuestCommand command) {
        if (command == null) {
            throw new InvalidProgressionCommandException("Completion command is required");
        }

        Quest quest = questRepository.findByIdForUpdate(command.questId())
                .orElseThrow(() -> new QuestNotFoundException(command.questId()));
        long playerId = quest.getAssignee().getId();
        Player player = playerRepository.findByIdForUpdate(playerId)
                .orElseThrow(() -> new DemoStateNotReadyException("Player " + playerId + " is not ready"));
        Raid raid = raidRepository.findFirstForUpdate()
                .orElseThrow(() -> new DemoStateNotReadyException("Raid is not ready"));

        if (quest.getStatus() == QuestStatus.DONE) {
            return resultForAlreadyCompleted(command, quest, player, raid);
        }

        int previousXp = player.getTotalXp();
        ProgressionRules.PlayerProgress previousProgress = progressionRules.describe(previousXp);
        int xpGained = quest.getXpReward();

        quest.complete();
        player.addXp(xpGained);
        raid.applyDamage(xpGained);

        ProgressionRules.PlayerProgress currentProgress = progressionRules.describe(player.getTotalXp());
        boolean levelUp = currentProgress.level() > previousProgress.level();
        CosmeticUnlock unlockedCosmetic = progressionRules
                .unlockedCosmetic(previousXp, player.getTotalXp())
                .orElse(null);

        return createResult(
                command.eventId(),
                true,
                null,
                xpGained,
                xpGained,
                levelUp,
                unlockedCosmetic,
                levelUp ? ProgressionResult.Reaction.LEVEL_UP : ProgressionResult.Reaction.HAPPY,
                quest,
                player,
                currentProgress,
                raid
        );
    }

    private ProgressionResult resultForAlreadyCompleted(
            CompleteQuestCommand command,
            Quest quest,
            Player player,
            Raid raid
    ) {
        return createResult(
                command.eventId(),
                false,
                ProgressionResult.Reason.ALREADY_COMPLETED,
                0,
                0,
                false,
                null,
                null,
                quest,
                player,
                progressionRules.describe(player.getTotalXp()),
                raid
        );
    }

    private ProgressionResult createResult(
            String eventId,
            boolean applied,
            ProgressionResult.Reason reason,
            int xpGained,
            int raidDamage,
            boolean levelUp,
            CosmeticUnlock unlockedCosmetic,
            ProgressionResult.Reaction reaction,
            Quest quest,
            Player player,
            ProgressionRules.PlayerProgress playerProgress,
            Raid raid
    ) {
        boolean hasActiveQuest = questRepository.existsByStatus(QuestStatus.IN_PROGRESS);
        boolean bossDefeated = raid.getCurrentHp() == 0;

        return new ProgressionResult(
                eventId,
                applied,
                reason,
                xpGained,
                raidDamage,
                levelUp,
                unlockedCosmetic,
                reaction,
                bossDefeated,
                new ProgressionResult.QuestSnapshot(
                        quest.getId(),
                        quest.getTitle(),
                        quest.getStatus(),
                        quest.getXpReward(),
                        player.getId(),
                        quest.getExternalReference()
                ),
                new ProgressionResult.PlayerSnapshot(
                        player.getId(),
                        player.getName(),
                        player.getTotalXp(),
                        playerProgress.level(),
                        playerProgress.nextLevelXp(),
                        playerProgress.title(),
                        playerProgress.cosmeticKey(),
                        hasActiveQuest ? ProgressionResult.CharacterState.CODING : ProgressionResult.CharacterState.IDLE
                ),
                new ProgressionResult.RaidSnapshot(
                        raid.getId(),
                        raid.getName(),
                        raid.getMaxHp(),
                        raid.getCurrentHp(),
                        bossDefeated ? ProgressionResult.RaidStatus.DEFEATED : ProgressionResult.RaidStatus.ACTIVE
                )
        );
    }
}
