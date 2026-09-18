package com.vibesprint.backend.player;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "player")
public class Player {

    @Id
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "total_xp", nullable = false)
    private int totalXp;

    protected Player() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getTotalXp() {
        return totalXp;
    }
}
