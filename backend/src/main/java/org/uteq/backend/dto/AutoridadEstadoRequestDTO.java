package org.uteq.backend.dto;

import jakarta.validation.constraints.NotNull;

public record AutoridadEstadoRequestDTO(
        @NotNull Boolean estado
) {}