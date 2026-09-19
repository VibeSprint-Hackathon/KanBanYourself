package com.vibesprint.backend.quest;

import com.vibesprint.backend.player.Player;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "quest")
public class Quest {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "quest_id_generator")
    @SequenceGenerator(name = "quest_id_generator", sequenceName = "quest_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private QuestStatus status;

    @Column(name = "xp_reward", nullable = false)
    private int xpReward;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignee_id", nullable = false)
    private Player assignee;

    @Column(name = "external_reference", length = 500)
    private String externalReference;

    @Column(name = "progress")
    private Integer progress;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected Quest() {
    }

    public static Quest create(
            String title,
            String description,
            QuestStatus status,
            Integer progress,
            int xpReward,
            Player assignee,
            String externalReference,
            int sortOrder
    ) {
        Quest quest = new Quest();
        quest.title = title;
        quest.description = description;
        quest.status = status;
        quest.progress = progress;
        quest.xpReward = xpReward;
        quest.assignee = assignee;
        quest.externalReference = externalReference;
        quest.sortOrder = sortOrder;
        return quest;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public QuestStatus getStatus() {
        return status;
    }

    public int getXpReward() {
        return xpReward;
    }

    public Player getAssignee() {
        return assignee;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public Integer getProgress() {
        return progress;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void assignTo(Player assignee) {
        if (assignee == null) {
            throw new IllegalArgumentException("Assignee cannot be null");
        }
        this.assignee = assignee;
    }

    public void updateEditableFields(String title, String description, String externalReference) {
        this.title = title;
        this.description = description;
        this.externalReference = externalReference;
    }

    public void updateUnfinished(
            String title,
            String description,
            QuestStatus status,
            Integer progress,
            int xpReward,
            String externalReference,
            int sortOrder
    ) {
        if (this.status == QuestStatus.DONE || status == QuestStatus.DONE) {
            throw new IllegalStateException("Completion must use the progression service");
        }
        updateEditableFields(title, description, externalReference);
        this.status = status;
        this.progress = progress;
        this.xpReward = xpReward;
        this.sortOrder = sortOrder;
    }

    public void moveTo(QuestStatus status, Integer progress) {
        if (this.status == QuestStatus.DONE || status == QuestStatus.DONE) {
            throw new IllegalStateException("Completed quests cannot be moved");
        }
        this.status = status;
        this.progress = progress;
    }

    public void reorder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void complete(int sortOrder) {
        if (status == QuestStatus.DONE) {
            throw new IllegalStateException("Quest is already completed");
        }
        this.status = QuestStatus.DONE;
        this.progress = 100;
        this.sortOrder = sortOrder;
    }
}
