package edu.uclm.esi.tysweb.bealquileres.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.esi.tysweb.bealquileres.dao.EstacionDao;
import edu.uclm.esi.tysweb.bealquileres.dao.MunicipioDao;
import edu.uclm.esi.tysweb.bealquileres.dao.VehiculoDao;
import edu.uclm.esi.tysweb.bealquileres.dto.VehiculoDto;
import edu.uclm.esi.tysweb.bealquileres.model.Estacion;
import edu.uclm.esi.tysweb.bealquileres.model.Municipio;
import edu.uclm.esi.tysweb.bealquileres.model.Vehiculo;

@Service 
public class VehiculoService {

    // RN-COM-04: Precio de referencia de cada bicicleta (1200 euros)
    public static final int PRECIO_BICICLETA = 1200;

    // RN-COM-02: Duración de la prerreserva temporal (30 minutos)
    public static final int MINUTOS_EXPIRACION_PRERRESERVA = 30;

    @Autowired
    private VehiculoDao vehiculoDao;

    @Autowired
    private MunicipioDao municipioDao;

    @Autowired
    private EstacionDao estacionDao;

    // -------------------------------------------------------------------------
    // RN-COM-04: Precio unitario de compra de bicicleta
    // -------------------------------------------------------------------------
    public int getPrecioDeBicicleta() {
        return PRECIO_BICICLETA;
    }

    public void createVehicles(Integer numberOfVehicles) {
        for (int i = 0; i < numberOfVehicles; i++) {
            Vehiculo v = new Vehiculo();
            vehiculoDao.save(v);
        }
    }

    // -------------------------------------------------------------------------
    // PRERRESERVA SINCRONIZADA (RN-COM-01, RN-COM-02, RC-CON-03, Anexo 11.3)
    // -------------------------------------------------------------------------
    public synchronized List<VehiculoDto> prerreservar(String nombreMunicipio, Integer cantidad) {
        if (cantidad == null || cantidad < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad de bicicletas debe ser mayor que 0");
        }

        if (nombreMunicipio == null || nombreMunicipio.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El municipio es obligatorio para prerreservar");
        }

        Municipio municipio = municipioDao.findByName(nombreMunicipio);
        if (municipio == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El municipio no existe en el sistema: " + nombreMunicipio);
        }

        // Bicicletas disponibles: sin municipio asignado (RN-COM-01) o con prerreserva caducada tras 30 min (RN-COM-02)
        LocalDateTime limiteExpiracion = LocalDateTime.now().minusMinutes(MINUTOS_EXPIRACION_PRERRESERVA);
        List<Vehiculo> disponibles = vehiculoDao.findDisponiblesParaPrerreserva(limiteExpiracion);

        if (disponibles.size() < cantidad) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No hay suficientes bicicletas disponibles para prerreservar. Solicitadas: " + cantidad +
                    ", disponibles: " + disponibles.size());
        }

        LocalDateTime ahora = LocalDateTime.now();
        List<VehiculoDto> resultado = new ArrayList<>();

        for (int i = 0; i < cantidad; i++) {
            Vehiculo bici = disponibles.get(i);
            bici.setMunicipio(municipio);
            bici.setHoraDePrerreserva(ahora);
            bici.setEstacion(null);
            vehiculoDao.save(bici);

            resultado.add(new VehiculoDto(
                    bici.getId(),
                    bici.getMatricula(),
                    bici.getLatitud(),
                    bici.getLongitud(),
                    bici.getDistanciaTotal(),
                    bici.getBateria(),
                    bici.getActivo(),
                    municipio.getName()
            ));
        }

        return resultado;
    }

    // -------------------------------------------------------------------------
    // CONFIRMACIÓN DEFINITIVA DE COMPRA (RN-COM-03, Anexo 11.3)
    // -------------------------------------------------------------------------
    public synchronized void confirmarCompra(String nombreMunicipio, Integer cantidad) {
        if (nombreMunicipio == null || nombreMunicipio.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Municipio no especificado");
        }

        List<Vehiculo> preasignadas = vehiculoDao.findByMunicipioNameAndHoraDePrerreservaIsNotNull(nombreMunicipio);

        if (preasignadas.isEmpty()) {
            // Si no hay con hora de prerreserva pero el municipio tiene vehículos, podemos verificar
            return;
        }

        int aConfirmar = (cantidad != null && cantidad > 0) ? Math.min(cantidad, preasignadas.size()) : preasignadas.size();

        for (int i = 0; i < aConfirmar; i++) {
            Vehiculo bici = preasignadas.get(i);
            // La asignación al municipio pasa a ser definitiva eliminando la hora de prerreserva (RN-COM-03)
            bici.setHoraDePrerreserva(null);
            vehiculoDao.save(bici);
        }
    }

    // Asignación directa tradicional de vehículos a municipio
    public synchronized void asignarVehiculos(String nombreMunicipio, Integer cantidad) {
        Municipio municipio = municipioDao.findByName(nombreMunicipio);
        if (municipio == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El municipio no existe en la base de datos: " + nombreMunicipio);
        }

        List<Vehiculo> vehiculosLibres = vehiculoDao.findByMunicipioIsNull();
        if (vehiculosLibres.size() < cantidad) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No hay suficientes bicicletas libres. Se requieren: " + cantidad + ", disponibles: " + vehiculosLibres.size());
        }

        for (int i = 0; i < cantidad; i++) {
            Vehiculo bici = vehiculosLibres.get(i);
            bici.setMunicipio(municipio);
            bici.setHoraDePrerreserva(null);
            vehiculoDao.save(bici);       
        }
    }

    public List<VehiculoDto> getVehiculos() {
        return vehiculoDao.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<VehiculoDto> getVehiculosPorMunicipio(String municipio) {
        if (municipio == null || municipio.isBlank()) {
            return getVehiculos();
        }
        return vehiculoDao.findByMunicipioName(municipio).stream()
                .filter(v -> !v.isPrerreservada())
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // RN-BIC-04: Gestión administrativa de bicicletas (activa/inactiva y observaciones)
    // -------------------------------------------------------------------------
    public synchronized VehiculoDto gestionarBicicleta(Long id, Boolean activo, String observaciones) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID de vehículo obligatorio");
        }

        Vehiculo v = vehiculoDao.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehículo no encontrado: " + id));

        if (activo != null) {
            v.setActivo(activo);
        }
        if (observaciones != null) {
            v.setObservaciones(observaciones);
        }
        vehiculoDao.save(v);

        return toDto(v);
    }

    // -------------------------------------------------------------------------
    // RN-EST-03, RC-CON-02: Ubicar bicicleta individual en estación de forma sincronizada
    // -------------------------------------------------------------------------
    public synchronized void ubicarVehiculoEnEstacion(Long vehiculoId, Long estacionId) {
        if (vehiculoId == null || estacionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vehículo y estación son requeridos");
        }

        Estacion estacion = estacionDao.findById(estacionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estación no encontrada: " + estacionId));

        int capacidad = estacion.getCapacidad() != null ? estacion.getCapacidad() : 10;
        int ocupadas = vehiculoDao.countByEstacionId(estacionId);

        Vehiculo v = vehiculoDao.findById(vehiculoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehículo no encontrado: " + vehiculoId));

        // Si el vehículo ya estaba en esta misma estación, no consume hueco extra
        boolean mismaEstacion = v.getEstacion() != null && v.getEstacion().getId().equals(estacionId);
        if (!mismaEstacion && ocupadas >= capacidad) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Capacidad máxima de la estación superada. Capacidad: " + capacidad + ", ocupadas: " + ocupadas);
        }

        v.setEstacion(estacion);
        v.setLatitud(estacion.getLatitude());
        v.setLongitud(estacion.getLongitude());
        vehiculoDao.save(v);
    }

    public synchronized void ubicarEnEstacion(Long estacionId, Integer cantidad) {
        if (estacionId == null || cantidad == null || cantidad < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estación y cantidad válidas son requeridas");
        }

        Estacion estacion = estacionDao.findById(estacionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estación no encontrada: " + estacionId));

        int capacidad = estacion.getCapacidad() != null ? estacion.getCapacidad() : 10;
        int ocupadas = vehiculoDao.countByEstacionId(estacionId);
        int huecosLibres = Math.max(0, capacidad - ocupadas);

        if (cantidad > huecosLibres) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, 
                "Capacidad máxima de la estación superada. Capacidad: " + capacidad + ", ocupadas: " + ocupadas + ", huecos libres: " + huecosLibres);
        }

        String municipioName = estacion.getMunicipio() != null ? estacion.getMunicipio().getName() : null;
        List<Vehiculo> libres = vehiculoDao.findByMunicipioNameAndEstacionIsNull(municipioName);

        if (libres.size() < cantidad) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, 
                "No hay suficientes bicicletas libres sin ubicar en " + (municipioName != null ? municipioName : "el municipio") + 
                " (" + libres.size() + " disponibles, se solicitaron " + cantidad + "). Asigna más bicicletas al municipio primero.");
        }

        for (int i = 0; i < cantidad; i++) {
            Vehiculo v = libres.get(i);
            v.setEstacion(estacion);
            v.setLatitud(estacion.getLatitude());
            v.setLongitud(estacion.getLongitude());
            vehiculoDao.save(v);
        }
    }

    public VehiculoDto toDto(Vehiculo v) {
        return new VehiculoDto(
                v.getId(),
                v.getMatricula(),
                v.getLatitud(),
                v.getLongitud(),
                v.getDistanciaTotal(),
                v.getBateria(),
                v.getActivo(),
                v.getMunicipio() != null ? v.getMunicipio().getName() : "Sin asignar",
                v.getObservaciones(),
                v.getEstacion() != null ? v.getEstacion().getId() : null,
                v.getEstacion() != null ? (v.getEstacion().getName() + " " + v.getEstacion().getNumber()) : null,
                v.getEstadoAsignacion(),
                v.isPrerreservada()
        );
    }
}
