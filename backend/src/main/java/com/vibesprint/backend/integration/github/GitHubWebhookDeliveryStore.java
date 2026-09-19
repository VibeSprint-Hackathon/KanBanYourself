package com.vibesprint.backend.integration.github;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class GitHubWebhookDeliveryStore {

    private final JdbcTemplate jdbcTemplate;

    public GitHubWebhookDeliveryStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean tryRegister(String deliveryId) {
        return jdbcTemplate.update(
                "insert into github_webhook_delivery (delivery_id) values (?) on conflict do nothing",
                deliveryId
        ) == 1;
    }
}
