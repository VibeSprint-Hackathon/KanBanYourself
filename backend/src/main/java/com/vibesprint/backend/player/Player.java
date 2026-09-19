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

    @Column(name = "github_login", unique = true, length = 100)
    private String githubLogin;

    @Column(name = "github_user_id", unique = true)
    private Long githubUserId;

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

    public String getGithubLogin() {
        return githubLogin;
    }

    public Long getGithubUserId() {
        return githubUserId;
    }

    public void addXp(int xp) {
        if (xp <= 0) {
            throw new IllegalArgumentException("XP gain must be positive");
        }
        totalXp = Math.addExact(totalXp, xp);
    }
}
