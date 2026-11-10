package edu.uclm.esi.tysweb.beusu.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank String email,
    @NotBlank String pwd
) {}
