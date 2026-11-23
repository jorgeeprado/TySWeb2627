package edu.uclm.esi.tysweb.bealquileres.dto;

public class DejarRequestDto {
    private Long vehiculoId;
    private String matricula;
    private Double latitud;
    private Double longitud;
    private Double distanciaRecorrida;
    private Double bateria;
    private Long estacionDestinoId;

    public DejarRequestDto() {}

    public DejarRequestDto(Long vehiculoId, String matricula, Double latitud, Double longitud, Double distanciaRecorrida, Double bateria, Long estacionDestinoId) {
        this.vehiculoId = vehiculoId;
        this.matricula = matricula;
        this.latitud = latitud;
        this.longitud = longitud;
        this.distanciaRecorrida = distanciaRecorrida;
        this.bateria = bateria;
        this.estacionDestinoId = estacionDestinoId;
    }

    public Long getVehiculoId() {
        return vehiculoId;
    }

    public void setVehiculoId(Long vehiculoId) {
        this.vehiculoId = vehiculoId;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public Double getLatitud() {
        return latitud;
    }

    public void setLatitud(Double latitud) {
        this.latitud = latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    public void setLongitud(Double longitud) {
        this.longitud = longitud;
    }

    public Double getDistanciaRecorrida() {
        return distanciaRecorrida;
    }

    public void setDistanciaRecorrida(Double distanciaRecorrida) {
        this.distanciaRecorrida = distanciaRecorrida;
    }

    public Double getBateria() {
        return bateria;
    }

    public void setBateria(Double bateria) {
        this.bateria = bateria;
    }

    public Long getEstacionDestinoId() {
        return estacionDestinoId;
    }

    public void setEstacionDestinoId(Long estacionDestinoId) {
        this.estacionDestinoId = estacionDestinoId;
    }
}
