package edu.uclm.esi.tysweb.bealquileres.dto;

public record EstacionDto(
    Long id,
    String name,
    String number,
    Double latitude,
    Double longitude,
    Integer capacidad,
    String municipio,
    Integer bicisOcupadas,
    Integer huecosLibres,
    String zipCode
) {}
