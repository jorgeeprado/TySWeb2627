package edu.uclm.esi.tysweb.bealquileres.dto;

/**
 * DTO para representar una bicicleta en el sistema
 */
public record VehiculoDto(
        Long id,
        String matricula,
        Double latitudActual,
        Double longitudActual,
        Double distanciaTotal,
        Double bateria,
        Boolean activo,
        String municipio,
        String observaciones,
        Long estacionId,
        String estacionNombre
) {
    // Constructor de compatibilidad hacia atrás
    public VehiculoDto(Long id, String matricula, Double latitudActual, Double longitudActual,
            Double distanciaTotal, Double bateria, Boolean activo, String municipio) {
        this(id, matricula, latitudActual, longitudActual, distanciaTotal, bateria, activo, municipio, null, null, null);
    }
}
