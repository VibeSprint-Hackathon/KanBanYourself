package com.vibesprint.backend.quest;

import com.vibesprint.backend.api.CreateQuestRequest;
import com.vibesprint.backend.api.DemoStateQueryService;
import com.vibesprint.backend.api.DemoStateResponse;
import com.vibesprint.backend.api.MoveQuestRequest;
import com.vibesprint.backend.api.UpdateQuestRequest;
import com.vibesprint.backend.player.Player;
import com.vibesprint.backend.player.PlayerRepository;
import com.vibesprint.backend.progression.DemoStateNotReadyException;
import com.vibesprint.backend.progression.QuestNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class QuestMutationService {

    private static final long DEMO_PLAYER_ID = 1L;
    private static final int ORDER_STEP = 100;

    private final QuestRepository questRepository;
    private final PlayerRepository playerRepository;
    private final DemoStateQueryService stateQueryService;

    public QuestMutationService(
            QuestRepository questRepository,
            PlayerRepository playerRepository,
            DemoStateQueryService stateQueryService
    ) {
        this.questRepository = questRepository;
        this.playerRepository = playerRepository;
        this.stateQueryService = stateQueryService;
    }

    @Transactional
    public DemoStateResponse create(CreateQuestRequest request) {
        rejectDoneDestination(request.status());
        Player assignee = playerRepository.findById(DEMO_PLAYER_ID)
                .orElseThrow(() -> new DemoStateNotReadyException("Player " + DEMO_PLAYER_ID + " is not ready"));
        Integer progress = normalizedProgress(request.status(), request.progress(), null);
        Quest quest = Quest.create(
                request.title(),
                request.description(),
                request.status(),
                progress,
                request.xpReward(),
                assignee,
                request.externalReference(),
                nextSortOrder(request.status())
        );
        questRepository.save(quest);
        return stateQueryService.getState();
    }

    @Transactional
    public DemoStateResponse update(long questId, UpdateQuestRequest request) {
        Quest quest = findForUpdate(questId);
        if (quest.getStatus() == QuestStatus.DONE) {
            updateCompleted(quest, request);
            return stateQueryService.getState();
        }

        rejectDoneDestination(request.status());
        Integer progress = normalizedProgress(request.status(), request.progress(), quest);
        int sortOrder = quest.getStatus() == request.status()
                ? quest.getSortOrder()
                : nextSortOrder(request.status());
        QuestStatus sourceStatus = quest.getStatus();
        quest.updateUnfinished(
                request.title(),
                request.description(),
                request.status(),
                progress,
                request.xpReward(),
                request.externalReference(),
                sortOrder
        );
        if (sourceStatus != request.status()) {
            normalizeColumn(sourceStatus, null);
        }
        return stateQueryService.getState();
    }

    @Transactional
    public DemoStateResponse move(long questId, MoveQuestRequest request) {
        rejectDoneDestination(request.status());
        Quest quest = findForUpdate(questId);
        if (quest.getStatus() == QuestStatus.DONE) {
            throw new QuestConflictException("Completed Quest " + questId + " cannot be moved");
        }

        QuestStatus sourceStatus = quest.getStatus();
        List<Quest> destination = new ArrayList<>(
                questRepository.findAllByStatusOrderBySortOrderAscIdAsc(request.status())
        );
        destination.removeIf(candidate -> candidate.getId().equals(questId));
        int insertionIndex = destination.size();
        if (request.beforeQuestId() != null) {
            insertionIndex = indexOf(destination, request.beforeQuestId());
        }

        quest.moveTo(request.status(), normalizedProgress(request.status(), null, quest));
        destination.add(insertionIndex, quest);
        normalize(destination);
        if (sourceStatus != request.status()) {
            normalizeColumn(sourceStatus, questId);
        }
        return stateQueryService.getState();
    }

    @Transactional
    public DemoStateResponse delete(long questId) {
        Quest quest = findForUpdate(questId);
        if (quest.getStatus() == QuestStatus.DONE) {
            throw new QuestConflictException("Completed Quest " + questId + " cannot be deleted");
        }
        QuestStatus sourceStatus = quest.getStatus();
        questRepository.delete(quest);
        questRepository.flush();
        normalizeColumn(sourceStatus, questId);
        return stateQueryService.getState();
    }

    private void updateCompleted(Quest quest, UpdateQuestRequest request) {
        if (request.status() != QuestStatus.DONE) {
            throw new QuestConflictException("Completed Quest status cannot be changed");
        }
        if (request.xpReward() != quest.getXpReward()) {
            throw new QuestConflictException("Completed Quest XP reward cannot be changed");
        }
        normalizedProgress(QuestStatus.DONE, request.progress(), quest);
        quest.updateEditableFields(request.title(), request.description(), request.externalReference());
    }

    private Quest findForUpdate(long questId) {
        return questRepository.findByIdForUpdate(questId)
                .orElseThrow(() -> new QuestNotFoundException(questId));
    }

    private void rejectDoneDestination(QuestStatus status) {
        if (status == QuestStatus.DONE) {
            throw new QuestConflictException("Quest completion must use the completion endpoint");
        }
    }

    private Integer normalizedProgress(QuestStatus status, Integer requested, Quest current) {
        return switch (status) {
            case BACKLOG, TODO -> {
                if (requested != null) {
                    throw new InvalidQuestRequestException(status + " Quest progress must be null");
                }
                yield null;
            }
            case IN_PROGRESS, TESTING -> {
                if (requested != null) {
                    yield requested;
                }
                yield current != null && current.getProgress() != null ? current.getProgress() : 0;
            }
            case DONE -> {
                if (requested == null || requested != 100) {
                    throw new InvalidQuestRequestException("DONE Quest progress must be 100");
                }
                yield 100;
            }
        };
    }

    private int nextSortOrder(QuestStatus status) {
        return questRepository.findAllByStatusOrderBySortOrderAscIdAsc(status).stream()
                .mapToInt(Quest::getSortOrder)
                .max()
                .orElse(0) + ORDER_STEP;
    }

    private int indexOf(List<Quest> quests, long beforeQuestId) {
        for (int index = 0; index < quests.size(); index++) {
            if (quests.get(index).getId() == beforeQuestId) {
                return index;
            }
        }
        throw new QuestNotFoundException(beforeQuestId);
    }

    private void normalizeColumn(QuestStatus status, Long excludedQuestId) {
        List<Quest> quests = questRepository.findAllByStatusOrderBySortOrderAscIdAsc(status).stream()
                .filter(quest -> excludedQuestId == null || !quest.getId().equals(excludedQuestId))
                .toList();
        normalize(quests);
    }

    private void normalize(List<Quest> quests) {
        for (int index = 0; index < quests.size(); index++) {
            quests.get(index).reorder((index + 1) * ORDER_STEP);
        }
    }
}
