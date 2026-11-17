package edu.uclm.esi.tysweb.beusu.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmPaymentRequest(
    @NotBlank(message = "El idTransaccion es obligatorio")
    String idTransaccion
) {}
