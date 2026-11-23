package edu.uclm.esi.tysweb.simulador.dto;

/**
 * VehiculoDto con información de estado, posición y porcentaje de batería (RN-BIC-01, RN-BIC-02)
 */
public record VehiculoDto(
        String matricula, 
        Double latitudOrigen, Double longitudOrigen, String nombreOrigen, 
        Double latitudDestino, Double longitudDestino, String nombreDestino,
        Double latitudActual, Double longitudActual, String direccion, 
        Double kmh, Double distanciaRecorrida, Double bateria) {
}
