package com.vibesprint.backend.api;

import com.vibesprint.backend.player.Player;
import com.vibesprint.backend.player.PlayerRepository;
import com.vibesprint.backend.progression.DemoStateNotReadyException;
import com.vibesprint.backend.progression.ProgressionRules;
import com.vibesprint.backend.quest.Quest;
import com.vibesprint.backend.quest.QuestRepository;
import com.vibesprint.backend.quest.QuestStatus;
import com.vibesprint.backend.raid.Raid;
import com.vibesprint.backend.raid.RaidRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DemoStateQueryService {

    private static final long DEMO_PLAYER_ID = 1L;

    private final PlayerRepository playerRepository;
    private final QuestRepository questRepository;
    private final RaidRepository raidRepository;
    private final ProgressionRules progressionRules;
    private final DemoApiMapper mapper;

    public DemoStateQueryService(
            PlayerRepository playerRepository,
            QuestRepository questRepository,
            RaidRepository raidRepository,
            ProgressionRules progressionRules,
            DemoApiMapper mapper
    ) {
        this.playerRepository = playerRepository;
        this.questRepository = questRepository;
        this.raidRepository = raidRepository;
        this.progressionRules = progressionRules;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public DemoStateResponse getState() {
        Player player = playerRepository.findById(DEMO_PLAYER_ID)
                .orElseThrow(() -> new DemoStateNotReadyException("Player " + DEMO_PLAYER_ID + " is not ready"));
        Raid raid = raidRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new DemoStateNotReadyException("Raid is not ready"));
        List<Quest> quests = questRepository.findAllInBoardOrder();
        boolean hasActiveQuest = quests.stream().anyMatch(quest -> quest.getStatus() == QuestStatus.IN_PROGRESS);
        ProgressionRules.PlayerProgress progress = progressionRules.describe(player.getTotalXp());

        return new DemoStateResponse(
                mapper.toPlayer(player, progress, hasActiveQuest),
                quests.stream().map(mapper::toQuest).toList(),
                mapper.toRaid(raid),
                progressionRules.nextUnlock(player.getTotalXp()).map(mapper::toUnlock).orElse(null)
        );
    }
}
