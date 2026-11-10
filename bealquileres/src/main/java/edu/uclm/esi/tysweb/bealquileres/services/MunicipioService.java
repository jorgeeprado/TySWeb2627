package edu.uclm.esi.tysweb.bealquileres.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.esi.tysweb.bealquileres.dao.EstacionDao;
import edu.uclm.esi.tysweb.bealquileres.dao.FeatureDao;
import edu.uclm.esi.tysweb.bealquileres.dao.MunicipioDao;
import edu.uclm.esi.tysweb.bealquileres.dto.FeatureDto;
import edu.uclm.esi.tysweb.bealquileres.model.Estacion;
import edu.uclm.esi.tysweb.bealquileres.model.Feature;
import edu.uclm.esi.tysweb.bealquileres.model.Municipio;

@Service
public class MunicipioService {

    private final CartoCiudadClient cartoCiudadClient;
    private final MunicipioDao municipioDao;
    private final FeatureDao featureDao;

    @Autowired 
    private SseService sseService;

    @Autowired
    private EstacionDao estacionDao;

    @Autowired
    private edu.uclm.esi.tysweb.bealquileres.dao.VehiculoDao vehiculoDao;

    public MunicipioService(CartoCiudadClient cartoCiudadClient, MunicipioDao municipioDao, FeatureDao featureDao) {
        this.cartoCiudadClient = cartoCiudadClient;
        this.municipioDao = municipioDao;
        this.featureDao = featureDao;
    }

    public void load(String name, Integer offset, Integer limit, Boolean progress) {
        List<FeatureDto> features = cartoCiudadClient.load(name, offset, limit);
        Municipio municipio = this.municipioDao.findByName(name);
        if (municipio==null) {
            municipio = new Municipio();
            municipio.setName(name);
            this.municipioDao.save(municipio);
        }

        final int BATCH_SIZE = 500;
        int processed = 0;
        List<Feature> batch = new ArrayList<>(BATCH_SIZE);

        for (FeatureDto dto : features) {
            Feature feature = new Feature();
            feature.setIdeeId(dto.id());

            Double c0 = dto.geometry() != null && dto.geometry().coordinates() != null && dto.geometry().coordinates().size() > 0 ? dto.geometry().coordinates().get(0) : null;
            Double c1 = dto.geometry() != null && dto.geometry().coordinates() != null && dto.geometry().coordinates().size() > 1 ? dto.geometry().coordinates().get(1) : null;

            // En GeoJSON c0 es longitud y c1 es latitud. Verificamos magnitudes para evitar inversión hacia África
            if (c0 != null && c1 != null) {
                if (Math.abs(c0) < Math.abs(c1)) {
                    feature.setLatitude(c1);
                    feature.setLongitude(c0);
                } else {
                    feature.setLatitude(c0);
                    feature.setLongitude(c1);
                }
            } else {
                feature.setLatitude(38.9861);
                feature.setLongitude(-3.9273);
            }

            feature.setMunicipio(municipio);
            feature.setName(dto.properties().componentThoroughfareName());
            feature.setNumber(dto.properties().locatorDesignatorAddressNumber());
            feature.setZipCode(dto.properties().componenPostalDescriptor());

            processed++;
            batch.add(feature);
            if (batch.size()==BATCH_SIZE) {
                this.featureDao.saveAllAndFlush(batch);
                if (progress)
                    this.sseService.send(name, "progress", processed);
                batch.clear();
            }
        }

        if (!batch.isEmpty())
            this.featureDao.saveAllAndFlush(batch);

        if (progress)
            this.sseService.send(name, "completed", processed);

        System.out.println("Terminado: " + processed);
    }

    public Integer getNumeroDeDirecciones(String municipio) {
        return this.cartoCiudadClient.getNumeroDeDirecciones(municipio);
    }

    public List<edu.uclm.esi.tysweb.bealquileres.dto.EstacionDto> getEstaciones(String municipioName) {
        return this.estacionDao.findByMunicipioName(municipioName).stream().map(e -> {
            int ocupadas = this.vehiculoDao != null ? this.vehiculoDao.countByEstacionId(e.getId()) : 0;
            int cap = e.getCapacidad() != null ? e.getCapacidad() : 10;
            int libres = Math.max(0, cap - ocupadas);
            return new edu.uclm.esi.tysweb.bealquileres.dto.EstacionDto(
                e.getId(),
                e.getName(),
                e.getNumber(),
                e.getLatitude(),
                e.getLongitude(),
                cap,
                e.getMunicipio() != null ? e.getMunicipio().getName() : municipioName,
                ocupadas,
                libres,
                e.getZipCode() != null ? e.getZipCode() : ""
            );
        }).toList();
    }

    public void setEstacion(String municipioName, String name, String number, Integer capacidad) {
        setEstacion(municipioName, name, number, capacidad, null, null, null);
    }

    public void setEstacion(String municipioName, String name, String number, Integer capacidad, Double latitude, Double longitude, String zipCode) {
        Feature feature = featureDao.findByMunicipioNameAndNameAndNumber(municipioName, name, number).orElse(null);

        Municipio municipio = this.municipioDao.findByName(municipioName);
        if (municipio == null) {
            municipio = new Municipio();
            municipio.setName(municipioName);
            this.municipioDao.save(municipio);
        }

        Double finalLat = latitude;
        Double finalLon = longitude;

        if (finalLat == null || finalLon == null) {
            if (feature != null && feature.getLatitude() != null && feature.getLongitude() != null) {
                finalLat = feature.getLatitude();
                finalLon = feature.getLongitude();
            } else {
                finalLat = 38.9861;
                finalLon = -3.9273;
            }
        }

        if (feature == null) {
            feature = new Feature();
            feature.setName(name);
            feature.setNumber(number);
            feature.setMunicipio(municipio);
            feature.setLatitude(finalLat);
            feature.setLongitude(finalLon);
            feature.setZipCode(zipCode);
            this.featureDao.save(feature);
        }

        Estacion estacion = new Estacion();
        estacion.setName(name);
        estacion.setNumber(number);
        estacion.setLatitude(finalLat);
        estacion.setLongitude(finalLon);
        estacion.setZipCode(zipCode != null ? zipCode : (feature.getZipCode() != null ? feature.getZipCode() : ""));
        estacion.setMunicipio(municipio);
        estacion.setCapacidad(capacidad != null ? capacidad : 10);

        estacionDao.save(estacion);
    }
}