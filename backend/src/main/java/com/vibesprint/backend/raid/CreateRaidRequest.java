package com.vibesprint.backend.raid;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateRaidRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 2000) String description,
        @Positive int maxHp,
        @Size(max = 500) String externalReference
) {
}
