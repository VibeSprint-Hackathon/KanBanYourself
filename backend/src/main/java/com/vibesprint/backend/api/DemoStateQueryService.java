package com.vibesprint.backend.api;

import com.vibesprint.backend.integration.github.GitHubRuntimeIssueStore;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DemoStateQueryService {

    private final PlayerRepository playerRepository;
    private final QuestRepository questRepository;
    private final RaidRepository raidRepository;
    private final ProgressionRules progressionRules;
    private final DemoApiMapper mapper;
    private final GitHubRuntimeIssueStore gitHubRuntimeIssueStore;

    public DemoStateQueryService(
            PlayerRepository playerRepository,
            QuestRepository questRepository,
            RaidRepository raidRepository,
            ProgressionRules progressionRules,
            DemoApiMapper mapper,
            GitHubRuntimeIssueStore gitHubRuntimeIssueStore
    ) {
        this.playerRepository = playerRepository;
        this.questRepository = questRepository;
        this.raidRepository = raidRepository;
        this.progressionRules = progressionRules;
        this.mapper = mapper;
        this.gitHubRuntimeIssueStore = gitHubRuntimeIssueStore;
    }

    @Transactional(readOnly = true)
    public DemoStateResponse getState() {
        List<Player> players = playerRepository.findAllByOrderByIdAsc();
        if (players.isEmpty()) {
            throw new DemoStateNotReadyException("Players are not ready");
        }

        Raid raid = raidRepository.findByStatus(com.vibesprint.backend.raid.RaidStatus.ACTIVE).orElse(null);
        List<Quest> quests = questRepository.findAllInBoardOrder();

        Map<String, DemoStateResponse.QuestView> byReference = new LinkedHashMap<>();

        for (Quest quest : quests) {
            putQuestView(byReference, mapper.toQuest(quest));
        }

        for (DemoStateResponse.QuestView runtimeQuest : gitHubRuntimeIssueStore.snapshot()) {
            putQuestView(byReference, runtimeQuest);
        }

        List<DemoStateResponse.QuestView> payloadQuests = new ArrayList<>(byReference.values());

        return new DemoStateResponse(
                mapPlayersFromViews(players, payloadQuests),
                payloadQuests,
                mapper.toRaid(raid)
        );
    }

    @Transactional(readOnly = true)
    public List<DemoStateResponse.PlayerView> getPlayers() {
        return mapPlayersFromEntities(playerRepository.findAllByOrderByIdAsc(), questRepository.findAllInBoardOrder());
    }

    private List<DemoStateResponse.PlayerView> mapPlayersFromEntities(List<Player> players, List<Quest> quests) {
        return players.stream().map(player -> {
            boolean hasActiveQuest = quests.stream().anyMatch(quest ->
                    quest.getAssignee() != null
                            && quest.getAssignee().getId().equals(player.getId())
                            && quest.getStatus() == QuestStatus.IN_PROGRESS
            );
            return mapper.toPlayer(player, progressionRules.describe(player.getTotalXp()), hasActiveQuest);
        }).toList();
    }

    private void putQuestView(Map<String, DemoStateResponse.QuestView> byReference, DemoStateResponse.QuestView questView) {
        String key = questView.id() > 0
                ? "id:" + questView.id()
                : questView.externalReference() != null && !questView.externalReference().isBlank()
                    ? "ref:" + questView.externalReference()
                    : "local:" + System.identityHashCode(questView);
        byReference.put(key, questView);
    }

    private List<DemoStateResponse.PlayerView> mapPlayersFromViews(List<Player> players, List<DemoStateResponse.QuestView> quests) {
        return players.stream().map(player -> {
            boolean hasActiveQuest = quests.stream().anyMatch(quest ->
                    quest.assigneeId() == player.getId()
                            && "IN_PROGRESS".equals(quest.status())
            );
            return mapper.toPlayer(player, progressionRules.describe(player.getTotalXp()), hasActiveQuest);
        }).toList();
    }
}
