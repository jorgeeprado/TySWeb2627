package edu.uclm.esi.tysweb.simulador.dto;

public class MoverVehiculoRequestDto {
    private Long vehiculoId;
    private String matricula;
    private String municipio;
    private Double origenLat;
    private Double origenLon;
    private String origenNombre;
    private Double destinoLat;
    private Double destinoLon;
    private String destinoNombre;
    private Long estacionDestinoId;
    private Double bateria;

    public MoverVehiculoRequestDto() {}

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

    public String getMunicipio() {
        return municipio;
    }

    public void setMunicipio(String municipio) {
        this.municipio = municipio;
    }

    public Double getOrigenLat() {
        return origenLat;
    }

    public void setOrigenLat(Double origenLat) {
        this.origenLat = origenLat;
    }

    public Double getOrigenLon() {
        return origenLon;
    }

    public void setOrigenLon(Double origenLon) {
        this.origenLon = origenLon;
    }

    public String getOrigenNombre() {
        return origenNombre;
    }

    public void setOrigenNombre(String origenNombre) {
        this.origenNombre = origenNombre;
    }

    public Double getDestinoLat() {
        return destinoLat;
    }

    public void setDestinoLat(Double destinoLat) {
        this.destinoLat = destinoLat;
    }

    public Double getDestinoLon() {
        return destinoLon;
    }

    public void setDestinoLon(Double destinoLon) {
        this.destinoLon = destinoLon;
    }

    public String getDestinoNombre() {
        return destinoNombre;
    }

    public void setDestinoNombre(String destinoNombre) {
        this.destinoNombre = destinoNombre;
    }

    public Long getEstacionDestinoId() {
        return estacionDestinoId;
    }

    public void setEstacionDestinoId(Long estacionDestinoId) {
        this.estacionDestinoId = estacionDestinoId;
    }

    public Double getBateria() {
        return bateria;
    }

    public void setBateria(Double bateria) {
        this.bateria = bateria;
    }
}
