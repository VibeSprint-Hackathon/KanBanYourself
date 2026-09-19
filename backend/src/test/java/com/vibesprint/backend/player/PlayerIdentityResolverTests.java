package com.vibesprint.backend.player;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class PlayerIdentityResolverTests {

    @Autowired
    private PlayerIdentityResolver resolver;

    @Test
    void prefersStableGithubUserIdOverLogin() {
        assertEquals(2L, resolver.resolve(22981929L, "Parsifal22").orElseThrow().getId());
    }

    @Test
    void fallsBackToCaseInsensitiveLogin() {
        assertEquals(3L, resolver.resolve(null, "pArSiFaL22").orElseThrow().getId());
    }

    @Test
    void doesNotGuessUnknownIdentity() {
        assertTrue(resolver.resolve(999999L, "unknown-user").isEmpty());
    }
}
