package edu.uclm.esi.tysweb.beusu.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SolicitarRecuperacionRequest(
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato de correo electrónico no es válido")
    String email
) {}
