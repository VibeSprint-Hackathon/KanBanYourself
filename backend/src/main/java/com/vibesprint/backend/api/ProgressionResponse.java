package com.vibesprint.backend.api;

public record ProgressionResponse(
        String eventId,
        boolean applied,
        String reason,
        int xpGained,
        int raidDamage,
        boolean levelUp,
        CosmeticView unlockedCosmetic,
        String reaction,
        boolean bossDefeated,
        DemoStateResponse.QuestView quest,
        DemoStateResponse.PlayerView player,
        DemoStateResponse.RaidView raid
) {

    public record CosmeticView(String key, String displayName) {
    }
}
