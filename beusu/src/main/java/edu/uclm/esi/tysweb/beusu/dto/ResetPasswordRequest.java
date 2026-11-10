package edu.uclm.esi.tysweb.beusu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(
    @NotBlank(message = "El token de recuperación es obligatorio")
    String token,

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,}$",
        message = "La contraseña debe tener al menos 8 caracteres y contener al menos una letra y un número"
    )
    String nuevaPwd1,

    @NotBlank(message = "La confirmación de la contraseña es obligatoria")
    String nuevaPwd2
) {}
