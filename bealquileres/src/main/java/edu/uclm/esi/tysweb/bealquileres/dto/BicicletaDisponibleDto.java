package edu.uclm.esi.tysweb.bealquileres.dto;

public record BicicletaDisponibleDto(
    Long id,
    String matricula,
    Double latitud,
    Double longitud,
    Double bateria,
    Long estacionId,
    String estacionNombre,
    Boolean activo,
    String observaciones
) {}
