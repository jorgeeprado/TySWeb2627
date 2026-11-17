package edu.uclm.esi.tysweb.beusu.dto;

public record PrepayRequest(
    Double importe,
    String tipo,
    Integer cantidad,
    String municipio
) {}
