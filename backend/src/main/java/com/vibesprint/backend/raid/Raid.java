package com.vibesprint.backend.raid;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "raid")
public class Raid {

    @Id
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "max_hp", nullable = false)
    private int maxHp;

    @Column(name = "current_hp", nullable = false)
    private int currentHp;

    protected Raid() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getCurrentHp() {
        return currentHp;
    }
}
