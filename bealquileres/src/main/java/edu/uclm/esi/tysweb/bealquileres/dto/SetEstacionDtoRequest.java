package edu.uclm.esi.tysweb.bealquileres.dto;

public record SetEstacionDtoRequest(
    String municipio,
    String name,
    String number,
    Integer capacidad,
    Double latitude,
    Double longitude,
    String zipCode
) {}
