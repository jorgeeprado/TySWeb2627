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
import edu.uclm.esi.tysweb.simulador.dto.Instruction;
import edu.uclm.esi.tysweb.simulador.dto.MoverVehiculoRequestDto;
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
        VehiculoList vehiculos = this.vehiculosPorMunicipio.get(city);
        if (vehiculos == null) {
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
                        v.getKmh(), v.getDistanciaRecorrida(), v.getBateria()))
                .toList();
    }

    public boolean moverVehiculo(MoverVehiculoRequestDto dto) {
        String city = dto.getMunicipio() != null ? dto.getMunicipio() : "Ciudad Real";

        Feature origen = new Feature();
        origen.setName(dto.getOrigenNombre() != null ? dto.getOrigenNombre() : "Origen");
        origen.setLatitude(dto.getOrigenLat() != null ? dto.getOrigenLat() : 38.9861);
        origen.setLongitude(dto.getOrigenLon() != null ? dto.getOrigenLon() : -3.9273);

        Feature destino = new Feature();
        destino.setName(dto.getDestinoNombre() != null ? dto.getDestinoNombre() : "Destino");
        destino.setLatitude(dto.getDestinoLat() != null ? dto.getDestinoLat() : 38.9861);
        destino.setLongitude(dto.getDestinoLon() != null ? dto.getDestinoLon() : -3.9273);

        int mat = 1;
        if (dto.getMatricula() != null) {
            try {
                mat = Integer.parseInt(dto.getMatricula());
            } catch (Exception e) {
                mat = (dto.getVehiculoId() != null) ? dto.getVehiculoId().intValue() : ultimaMatricula++;
            }
        } else if (dto.getVehiculoId() != null) {
            mat = dto.getVehiculoId().intValue();
        } else {
            mat = ultimaMatricula++;
        }

        Vehiculo v = new Vehiculo(mat, origen, destino);
        v.setVehiculoId(dto.getVehiculoId());
        v.setEstacionDestinoId(dto.getEstacionDestinoId());
        if (dto.getBateria() != null) {
            v.setBateria(dto.getBateria());
        }

        List<Instruction> instructions = null;
        try {
            RouteResponse route = getRoute(origen, destino);
            if (route != null && route.instructionsData() != null) {
                instructions = route.instructionsData().instruction();
            }
        } catch (Exception e) {
            System.out.println("CartoCiudad no disponible, generando ruta sintética directa: " + e.getMessage());
        }

        if (instructions == null || instructions.isEmpty()) {
            instructions = generarRutaSintetica(origen, destino);
        }

        v.setRuta(instructions);
        VehiculoList vehiculos = this.getVehiculosEnCiudad(city);
        vehiculos.add(v);
        v.arrancar(() -> { vehiculos.remove(v); });

        return true;
    }

    private List<Instruction> generarRutaSintetica(Feature origen, Feature destino) {
        List<Instruction> list = new ArrayList<>();
        int steps = 8;
        double origLat = origen.getLatitude();
        double origLon = origen.getLongitude();
        double destLat = destino.getLatitude();
        double destLon = destino.getLongitude();

        String[] calles = {
            "Calle Toledo", "Calle Calatrava", "Calle Alarcos", 
            "Plaza Mayor", "Calle Ciruela", "Ronda de Alarcos", 
            "Paseo del Prado", "Avenida del Torreón"
        };

        for (int i = 0; i < steps; i++) {
            double startFraction = (double) i / steps;
            double endFraction = (double) (i + 1) / steps;

            double p1Lon = origLon + (destLon - origLon) * startFraction;
            double p1Lat = origLat + (destLat - origLat) * startFraction;
            double p2Lon = origLon + (destLon - origLon) * endFraction;
            double p2Lat = origLat + (destLat - origLat) * endFraction;

            double distMetros = 150.0;
            String desc = calles[i % calles.length];

            Instruction inst = new Instruction(
                List.of(),
                desc,
                List.of(p2Lon, p2Lat),
                Math.round(distMetros) + " m",
                "CONTINUE",
                List.of(p1Lon, p1Lat)
            );
            list.add(inst);
        }
        return list;
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
