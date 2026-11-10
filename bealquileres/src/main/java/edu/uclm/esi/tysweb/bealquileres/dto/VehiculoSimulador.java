package edu.uclm.esi.tysweb.bealquileres.dto;

public class VehiculoSimulador {
    private String matricula;
    private Double latitudActual;
    private Double longitudActual;
    private Double latitudOrigen;
    private Double longitudOrigen;
    private Double latitudDestino;
    private Double longitudDestino;
    private String nombreOrigen;
    private String nombreDestino;
    private String direccion;
    private Double kmh;
    private Double distanciaRecorrida;
    private Double bateria = 100.0;
    private String municipio;

    public VehiculoSimulador() {}

    public VehiculoSimulador(String matricula, Double latitudActual, Double longitudActual, Double bateria, String municipio) {
        this.matricula = matricula;
        this.latitudActual = latitudActual;
        this.longitudActual = longitudActual;
        this.bateria = bateria != null ? bateria : 100.0;
        this.municipio = municipio;
    }

    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    public Double getLatitudActual() { return latitudActual; }
    public void setLatitudActual(Double latitudActual) { this.latitudActual = latitudActual; }

    public Double getLongitudActual() { return longitudActual; }
    public void setLongitudActual(Double longitudActual) { this.longitudActual = longitudActual; }

    public Double getLatitudOrigen() { return latitudOrigen; }
    public void setLatitudOrigen(Double latitudOrigen) { this.latitudOrigen = latitudOrigen; }

    public Double getLongitudOrigen() { return longitudOrigen; }
    public void setLongitudOrigen(Double longitudOrigen) { this.longitudOrigen = longitudOrigen; }

    public Double getLatitudDestino() { return latitudDestino; }
    public void setLatitudDestino(Double latitudDestino) { this.latitudDestino = latitudDestino; }

    public Double getLongitudDestino() { return longitudDestino; }
    public void setLongitudDestino(Double longitudDestino) { this.longitudDestino = longitudDestino; }

    public String getNombreOrigen() { return nombreOrigen; }
    public void setNombreOrigen(String nombreOrigen) { this.nombreOrigen = nombreOrigen; }

    public String getNombreDestino() { return nombreDestino; }
    public void setNombreDestino(String nombreDestino) { this.nombreDestino = nombreDestino; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public Double getKmh() { return kmh; }
    public void setKmh(Double kmh) { this.kmh = kmh; }

    public Double getDistanciaRecorrida() { return distanciaRecorrida; }
    public void setDistanciaRecorrida(Double distanciaRecorrida) { this.distanciaRecorrida = distanciaRecorrida; }

    public Double getBateria() { return bateria; }
    public void setBateria(Double bateria) { this.bateria = bateria; }

    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }
}

