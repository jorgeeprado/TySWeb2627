package edu.uclm.esi.tysweb.bealquileres.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity 
public class Vehiculo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double latitud;
    private Double longitud;

    private Double distanciaTotal = 0.0;
    private Double bateria = 100.0;

    private Boolean activo = true;

    // Observaciones administrativas (RN-BIC-04, ej. "pinchada", "robada", "en taller")
    @Column(length = 500)
    private String observaciones;

    // Hora en la que se realizó la prerreserva temporal (RN-COM-02: 30 minutos de vigencia)
    private LocalDateTime horaDePrerreserva;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "municipio_id", nullable = true)
    private Municipio municipio;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "estacion_id", nullable = true)
    private Estacion estacion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Double getDistanciaTotal() {
        return distanciaTotal;
    }

    public void setDistanciaTotal(Double distanciaTotal) {
        this.distanciaTotal = distanciaTotal;
    }

    public Double getBateria() {
        return bateria;
    }

    public void setBateria(Double bateria) {
        this.bateria = bateria;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public LocalDateTime getHoraDePrerreserva() {
        return horaDePrerreserva;
    }

    public void setHoraDePrerreserva(LocalDateTime horaDePrerreserva) {
        this.horaDePrerreserva = horaDePrerreserva;
    }

    public String getMatricula() {
        return String.format("%04d", this.id);
    }

    public Municipio getMunicipio() {
        return municipio;
    }

    public void setMunicipio(Municipio municipio) {
        this.municipio = municipio;
    }

    public Estacion getEstacion() {
        return estacion;
    }

    public void setEstacion(Estacion estacion) {
        this.estacion = estacion;
    }
}
