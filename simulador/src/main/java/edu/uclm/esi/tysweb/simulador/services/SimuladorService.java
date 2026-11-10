package edu.uclm.esi.tysweb.simulador.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.esi.tysweb.simulador.dao.FeatureDao;
import edu.uclm.esi.tysweb.simulador.dao.MunicipioDao;
import edu.uclm.esi.tysweb.simulador.dto.GeneracionDto;
import edu.uclm.esi.tysweb.simulador.dto.GeneracionElementDto;
import edu.uclm.esi.tysweb.simulador.dto.MunicipioDto;
import edu.uclm.esi.tysweb.simulador.dto.PuntoRutaDto;
import edu.uclm.esi.tysweb.simulador.dto.RouteResponse;
import edu.uclm.esi.tysweb.simulador.dto.VehiculoDto;
import edu.uclm.esi.tysweb.simulador.model.Feature;
import edu.uclm.esi.tysweb.simulador.model.Municipio;
import edu.uclm.esi.tysweb.simulador.model.Vehiculo;
import edu.uclm.esi.tysweb.simulador.model.VehiculoList;

@Service
public class SimuladorService {
    @Autowired
    private CartoCiudadRouteClient cartoCiudadRouteClient;
    @Autowired
    private MunicipioDao municipioDao;
    @Autowired
    private FeatureDao featureDao;

    private int ultimaMatricula = 1;
    private final static int MAX = 20;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private Map<String, VehiculoList> vehiculosPorMunicipio = new ConcurrentHashMap<>();

    public void simular(String city, boolean random) {
        if (random) 
            this.simulacionAleatoria(city);
        else 
            this.simulacionFija();
    }

    private void simulacionFija() {
        Feature origen = this.featureDao.findById("6b946b19-6939-4b7d-908a-4a55ce10ab1f").get();
        Feature destino = this.featureDao.findById("4523882d-ae38-4d17-907c-567f1600bbb3").get();
        RouteResponse route = this.getRoute(origen, destino);

        VehiculoList vehiculos = this.getVehiculosEnCiudad("Ciudad Real");
        Vehiculo v = new Vehiculo(ultimaMatricula++, origen, destino);
        v.setRuta(route.instructionsData().instruction());
        vehiculos.add(v);
        v.arrancar(() -> { vehiculos.remove(v);});
    }

    public void simulacionAleatoria(String city) {
        VehiculoList vehiculos = this.getVehiculosEnCiudad(city);

        this.scheduler.scheduleAtFixedRate(() -> {
            if (vehiculos.size() < MAX) {
                generateBicycle(city);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public GeneracionDto generateBicycle(String city) {
        GeneracionDto result = new GeneracionDto(city);
        Municipio municipio = this.municipioDao.findByName(city);
        if (municipio==null) {
            result.setResponseType("error");
            result.setResponse("No se encuentra el municipio " + city);
            return result;
        }

        result.setResponseType("todo OK");
        Feature origen, destino;
        origen = this.featureDao.findRandomByMunicipio(municipio.getId());
        destino = null;
        do {
            destino = this.featureDao.findRandomByMunicipio(municipio.getId());
        } while (destino.getId().equals(origen.getId()));

        Vehiculo vehiculo = new Vehiculo(ultimaMatricula++, origen, destino);
        GeneracionElementDto element = new GeneracionElementDto(ultimaMatricula-1);
        try {
            RouteResponse ruta = getRoute(origen, destino);
            vehiculo.setRuta(ruta.instructionsData().instruction());
            VehiculoList vehiculos = this.getVehiculosEnCiudad(city);
            vehiculos.add(vehiculo);
            vehiculo.arrancar(() -> { vehiculos.remove(vehiculo);});
            element.setArrancado(true);
            
        } catch (Exception e) {
            result.setResponseType("algún error");
            element.setArrancado(false);
            element.setError(e.getMessage());
        }
        result.addElement(element);
        return result;
    }

    private VehiculoList getVehiculosEnCiudad(String city) {
        Municipio municipio = this.municipioDao.findByName(city);
        if (municipio==null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Municipio no encontrado (" + city + ")");

        VehiculoList vehiculos = this.vehiculosPorMunicipio.get(city);
        if (vehiculos==null) {
            vehiculos = new VehiculoList();
            this.vehiculosPorMunicipio.put(city, vehiculos);
        }
        return vehiculos;
    }

    private RouteResponse getRoute(Feature origen, Feature destino) {
        RouteResponse route = cartoCiudadRouteClient.getRoute(
                origen.getLatitude(),
                origen.getLongitude(),
                destino.getLatitude(),
                destino.getLongitude());
        return route;
    }

    public List<VehiculoDto> getVehiculos(String city) {
        VehiculoList vehiculos = this.getVehiculosEnCiudad(city);
        return vehiculos.getVehiculos().stream()
                .map(v -> new VehiculoDto(
                        String.format("%04d", v.getMatricula()),
                        v.getOrigen().getLatitude(), v.getOrigen().getLongitude(), v.getOrigen().getName(),
                        v.getDestino().getLatitude(), v.getDestino().getLongitude(), v.getDestino().getName(),
                        v.getCurrentLatitude(), v.getCurrentLongitude(), v.getCurrentStreet(),
                        v.getKmh(), v.getDistanciaRecorrida()))
                .toList();
    }

    private Vehiculo findVehiculo(String city, int matricula) {
        VehiculoList vehiculos = this.getVehiculosEnCiudad(city);
        return vehiculos.getVehiculos().stream()
                .filter(v -> v.getMatricula()==matricula)
                .findFirst()
                .orElse(null);
    }

    public List<PuntoRutaDto> getRuta(String city, int matricula) {
        Vehiculo vehiculo = this.findVehiculo(city, matricula);
        if (vehiculo == null) 
            return List.of();
        return vehiculo.getRuta();
    }

    public List<MunicipioDto> getCities() {
        return this.municipioDao.findAll().stream()
            .map(m -> new MunicipioDto(
                    m.getName(),
                    m.getLatitude() != null ? m.getLatitude() : 38.9861,
                    m.getLongitude() != null ? m.getLongitude() : -3.9273
            )).toList();
    }
}
