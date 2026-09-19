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
        Raid raid = raidRepository.findByStatus(com.vibesprint.backend.raid.RaidStatus.ACTIVE).orElse(null);
        List<Quest> quests = questRepository.findAllInBoardOrder();
        List<Player> players = playerRepository.findAllByOrderByIdAsc();
        if (players.isEmpty()) {
            throw new DemoStateNotReadyException("Players are not ready");
        }

        return new DemoStateResponse(
                mapPlayers(players, quests),
                quests.stream().map(mapper::toQuest).toList(),
                mapper.toRaid(raid)
        );
    }

    @Transactional(readOnly = true)
    public List<DemoStateResponse.PlayerView> getPlayers() {
        return mapPlayers(playerRepository.findAllByOrderByIdAsc(), questRepository.findAllInBoardOrder());
    }

    private List<DemoStateResponse.PlayerView> mapPlayers(List<Player> players, List<Quest> quests) {
        return players.stream().map(player -> {
            boolean hasActiveQuest = quests.stream().anyMatch(quest ->
                    quest.getAssignee().getId().equals(player.getId())
                            && quest.getStatus() == QuestStatus.IN_PROGRESS
            );
            return mapper.toPlayer(player, progressionRules.describe(player.getTotalXp()), hasActiveQuest);
        }).toList();
    }
}
