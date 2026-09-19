package com.vibesprint.backend.api;

import com.vibesprint.backend.player.Player;
import com.vibesprint.backend.progression.CosmeticUnlock;
import com.vibesprint.backend.progression.ProgressionResult;
import com.vibesprint.backend.progression.ProgressionRules;
import com.vibesprint.backend.quest.Quest;
import com.vibesprint.backend.raid.Raid;
import org.springframework.stereotype.Component;

@Component
public class DemoApiMapper {

    public ProgressionResponse toResponse(ProgressionResult result) {
        return toResponse(result, "DEMO", false);
    }

    public ProgressionResponse toResponse(ProgressionResult result, String source, boolean duplicate) {
        return new ProgressionResponse(
                result.eventId(),
                result.applied(),
                enumName(result.reason()),
                result.xpGained(),
                result.raidDamage(),
                result.levelUp(),
                toCosmetic(result.unlockedCosmetic()),
                reactionValue(result.reaction()),
                result.bossDefeated(),
                toQuest(result.quest()),
                toPlayer(result.player()),
                toRaid(result.raid()),
                source,
                duplicate
        );
    }

    public DemoStateResponse.QuestView toQuest(Quest quest) {
        return new DemoStateResponse.QuestView(
                quest.getId(),
                quest.getTitle(),
                quest.getDescription(),
                quest.getStatus().name(),
                quest.getProgress(),
                quest.getXpReward(),
                quest.getAssignee().getId(),
                quest.getExternalReference(),
                quest.getSortOrder()
        );
    }

    public DemoStateResponse.PlayerView toPlayer(
            Player player,
            ProgressionRules.PlayerProgress progress,
            boolean hasActiveQuest
    ) {
        return new DemoStateResponse.PlayerView(
                player.getId(),
                player.getName(),
                player.getTotalXp(),
                progress.level(),
                progress.nextLevelXp(),
                progress.title(),
                progress.cosmeticKey(),
                hasActiveQuest ? "coding" : "idle"
        );
    }

    public DemoStateResponse.RaidView toRaid(Raid raid) {
        return new DemoStateResponse.RaidView(
                raid.getId(),
                raid.getName(),
                raid.getMaxHp(),
                raid.getCurrentHp(),
                raid.getCurrentHp() == 0 ? "DEFEATED" : "ACTIVE"
        );
    }

    public DemoStateResponse.UnlockView toUnlock(ProgressionRules.UnlockTarget unlock) {
        return new DemoStateResponse.UnlockView(
                unlock.level(),
                unlock.cosmetic().key(),
                unlock.cosmetic().displayName()
        );
    }

    private DemoStateResponse.QuestView toQuest(ProgressionResult.QuestSnapshot quest) {
        return new DemoStateResponse.QuestView(
                quest.id(),
                quest.title(),
                quest.description(),
                quest.status().name(),
                quest.progress(),
                quest.xpReward(),
                quest.assigneeId(),
                quest.externalReference(),
                quest.sortOrder()
        );
    }

    private DemoStateResponse.PlayerView toPlayer(ProgressionResult.PlayerSnapshot player) {
        return new DemoStateResponse.PlayerView(
                player.id(),
                player.name(),
                player.totalXp(),
                player.level(),
                player.nextLevelXp(),
                player.title(),
                player.cosmeticKey(),
                characterStateValue(player.characterState())
        );
    }

    private DemoStateResponse.RaidView toRaid(ProgressionResult.RaidSnapshot raid) {
        return new DemoStateResponse.RaidView(
                raid.id(),
                raid.name(),
                raid.maxHp(),
                raid.currentHp(),
                raid.status().name()
        );
    }

    private ProgressionResponse.CosmeticView toCosmetic(CosmeticUnlock cosmetic) {
        return cosmetic == null ? null : new ProgressionResponse.CosmeticView(cosmetic.key(), cosmetic.displayName());
    }

    private String reactionValue(ProgressionResult.Reaction reaction) {
        if (reaction == null) {
            return null;
        }
        return switch (reaction) {
            case HAPPY -> "happy";
            case LEVEL_UP -> "level-up";
        };
    }

    private String characterStateValue(ProgressionResult.CharacterState characterState) {
        return switch (characterState) {
            case IDLE -> "idle";
            case CODING -> "coding";
        };
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
