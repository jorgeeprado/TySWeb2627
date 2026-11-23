package edu.uclm.esi.tysweb.bealquileres.dto;

import java.time.LocalDateTime;

public record AlquilerResponseDto(
    Long id,
    String usuarioEmail,
    Long vehiculoId,
    String matricula,
    Long estacionOrigenId,
    String estacionOrigenNombre,
    Long estacionDestinoId,
    String estacionDestinoNombre,
    LocalDateTime fechaInicio,
    LocalDateTime fechaFin,
    Boolean activo,
    Double distanciaRecorrida,
    Double bateria
) {}
