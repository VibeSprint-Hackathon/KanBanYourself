package com.vibesprint.backend.player;

import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PlayerIdentityResolver {

    private final PlayerRepository playerRepository;

    public PlayerIdentityResolver(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public Optional<Player> resolve(Long githubUserId, String githubLogin) {
        if (githubUserId != null) {
            Optional<Player> byId = playerRepository.findByGithubUserId(githubUserId);
            if (byId.isPresent()) {
                return byId;
            }
        }
        if (githubLogin == null || githubLogin.isBlank()) {
            return Optional.empty();
        }
        return playerRepository.findByGithubLoginIgnoreCase(githubLogin.trim());
    }
}
