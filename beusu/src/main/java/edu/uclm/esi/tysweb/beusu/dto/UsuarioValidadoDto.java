package edu.uclm.esi.tysweb.beusu.dto;

/**
 * DTO devuelto por el endpoint interno /users/validateSession
 * Cumple con RT-AUT-02 y RC-SEC-02 para la comunicación inter-backend.
 */
public record UsuarioValidadoDto(
    String id,
    String email,
    String nombre,
    String rol,
    String municipio,
    boolean pagadoAlta
) {}
