package edu.uclm.esi.tysweb.beusu.dto;

public record LoginResponse(
    String token,
    String email,
    String nombre,
    String municipio,
    String rol,
    boolean fa2Activated
) {}
