package com.vibesprint.backend.integration.github;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class GitHubApiConfiguration {

    @Bean
    public RestTemplate githubRestTemplate() {
        return new RestTemplate();
    }
}
