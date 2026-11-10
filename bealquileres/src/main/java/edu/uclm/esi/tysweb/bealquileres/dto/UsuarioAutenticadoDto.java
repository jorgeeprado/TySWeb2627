package edu.uclm.esi.tysweb.bealquileres.dto;

/**
 * DTO que representa los datos del usuario autenticado devueltos por beusu.
 * Cumple con RT-AUT-02 y RC-SEC-02 para el control de acceso en bealquileres.
 */
public record UsuarioAutenticadoDto(
    String id,
    String email,
    String nombre,
    String rol,
    String municipio,
    boolean pagadoAlta
) {}
