package edu.uclm.esi.tysweb.bealquileres.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.esi.tysweb.bealquileres.dao.MunicipioDao;
import edu.uclm.esi.tysweb.bealquileres.dao.VehiculoDao;
import edu.uclm.esi.tysweb.bealquileres.dto.VehiculoDto;
import edu.uclm.esi.tysweb.bealquileres.model.Municipio;
import edu.uclm.esi.tysweb.bealquileres.model.Vehiculo;

@Service 
public class VehiculoService {

    @Autowired
    private VehiculoDao vehiculoDao;

    @Autowired
    private MunicipioDao municipioDao;

    @Autowired
    private edu.uclm.esi.tysweb.bealquileres.dao.EstacionDao estacionDao;

    public void createVehicles(Integer numberOfVehicles) {
        for (int i = 0; i < numberOfVehicles; i++) {
            Vehiculo v = new Vehiculo();
            vehiculoDao.save(v);
        }
    }

    // Implementación exacta del diagrama de secuencia
    public synchronized void asignarVehiculos(String nombreMunicipio, Integer cantidad) {
        
        // A) Comprobar si el municipio existe (Lanza 404 si no)
        Municipio municipio = municipioDao.findByName(nombreMunicipio);
        if (municipio == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El municipio no existe en la base de datos: " + nombreMunicipio);
        }

        // B) Comprobar si hay bicis suficientes (Lanza CONFLICT si no)
        List<Vehiculo> vehiculosLibres = vehiculoDao.findByMunicipioIsNull();
        if (vehiculosLibres.size() < cantidad) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No hay suficientes bicicletas libres. Se requieren: " + cantidad + ", disponibles: " + vehiculosLibres.size());
        }

        // C) Asignar las bicicletas al municipio
        for (int i = 0; i < cantidad; i++) {
            Vehiculo bici = vehiculosLibres.get(i);
            bici.setMunicipio(municipio); 
            vehiculoDao.save(bici);       
        }
    }

    public List<VehiculoDto> getVehiculos() {
        return vehiculoDao.findAll().stream().map(v -> new VehiculoDto(
            v.getId(), 
            v.getMatricula(), 
            v.getLatitud(), 
            v.getLongitud(), 
            v.getDistanciaTotal(), 
            v.getBateria(), 
            v.getActivo(), 
            v.getMunicipio() != null ? v.getMunicipio().getName() : "Sin asignar"
        )).collect(Collectors.toList());
    }

    public synchronized void ubicarEnEstacion(Long estacionId, Integer cantidad) {
        if (estacionId == null || cantidad == null || cantidad < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estación y cantidad válidas son requeridas");
        }

        edu.uclm.esi.tysweb.bealquileres.model.Estacion estacion = estacionDao.findById(estacionId)
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
}