package edu.uclm.esi.tysweb.bealquileres.dto;

/**
 * DTO simplificado para listar puntos de interés (Feature) en el frontend
 */
public record FeatureSimpleDto(
        String id,
        String name,
        String number,
        Double latitude,
        Double longitude,
        String zipCode
) {
}
