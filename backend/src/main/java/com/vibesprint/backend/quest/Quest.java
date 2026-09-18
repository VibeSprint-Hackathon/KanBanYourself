package com.vibesprint.backend.quest;

import com.vibesprint.backend.player.Player;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "quest")
public class Quest {

    @Id
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

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

    protected Quest() {
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
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
}
