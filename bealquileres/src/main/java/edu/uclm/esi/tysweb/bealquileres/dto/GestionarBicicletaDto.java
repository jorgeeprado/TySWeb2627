package edu.uclm.esi.tysweb.bealquileres.dto;

/**
 * DTO para la gestión administrativa de bicicletas (RN-BIC-04)
 */
public record GestionarBicicletaDto(
        Long id,
        Boolean activo,
        String observaciones
) {
}
