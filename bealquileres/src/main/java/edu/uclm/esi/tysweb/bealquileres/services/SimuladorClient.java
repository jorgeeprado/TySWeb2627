package edu.uclm.esi.tysweb.bealquileres.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import edu.uclm.esi.tysweb.bealquileres.dao.VehiculoDao;
import edu.uclm.esi.tysweb.bealquileres.dto.VehiculoSimulador;
import edu.uclm.esi.tysweb.bealquileres.model.Vehiculo;

@Service
public class SimuladorClient {

    @Value("${simulador.url:http://localhost:8082}")
    private String simuladorUrl;

    @Autowired(required = false)
    private VehiculoDao vehiculoDao;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(1500))
            .build();

    public List<VehiculoSimulador> getVehiculos(String municipio) {
        List<VehiculoSimulador> vehiculos = new ArrayList<>();

        try {
            String encodedCity = java.net.URLEncoder.encode(municipio, java.nio.charset.StandardCharsets.UTF_8);
            String url = this.simuladorUrl + "/simulador/getVehiculos/" + encodedCity;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(2000))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body() != null && !response.body().isBlank()) {
                JSONArray array = new JSONArray(response.body());
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    VehiculoSimulador vs = new VehiculoSimulador();
                    vs.setMatricula(obj.optString("matricula", String.format("%04d", i + 1)));
                    vs.setLatitudActual(obj.optDouble("latitudActual", 38.9861));
                    vs.setLongitudActual(obj.optDouble("longitudActual", -3.9273));
                    vs.setLatitudOrigen(obj.optDouble("latitudOrigen", 38.9861));
                    vs.setLongitudOrigen(obj.optDouble("longitudOrigen", -3.9273));
                    vs.setLatitudDestino(obj.optDouble("latitudDestino", 38.9861));
                    vs.setLongitudDestino(obj.optDouble("longitudDestino", -3.9273));
                    vs.setNombreOrigen(obj.optString("nombreOrigen", ""));
                    vs.setNombreDestino(obj.optString("nombreDestino", ""));
                    vs.setDireccion(obj.optString("direccion", ""));
                    vs.setKmh(obj.optDouble("kmh", 15.0));
                    vs.setDistanciaRecorrida(obj.optDouble("distanciaRecorrida", 0.0));
                    
                    // Cálculo de batería según distancia recorrida (RN-BIC-02)
                    double bat = Math.max(10.0, 100.0 - (vs.getDistanciaRecorrida() / 100.0));
                    vs.setBateria(Math.round(bat * 10.0) / 10.0);
                    vs.setMunicipio(municipio);

                    vehiculos.add(vs);
                }
            }
        } catch (Exception e) {
            // Manejo de dependencias sin bloquear (RC-DISP-01, DA-EXT-01)
            System.out.println("Aviso: No se pudo contactar con simulador (" + e.getMessage() + "). Recuperando desde BD local.");
        }

        // Si el simulador aún no tiene bicicletas simuladas en movimiento para este municipio,
        // devolvemos las bicicletas reales asignadas en la base de datos de bealquileres
        if (vehiculos.isEmpty() && vehiculoDao != null) {
            try {
                List<Vehiculo> locales = vehiculoDao.findByMunicipioName(municipio);
                for (Vehiculo v : locales) {
                    Double lat = v.getLatitud() != null ? v.getLatitud() : 38.9861;
                    Double lon = v.getLongitud() != null ? v.getLongitud() : -3.9273;
                    VehiculoSimulador vs = new VehiculoSimulador(
                        v.getMatricula(),
                        lat,
                        lon,
                        v.getBateria() != null ? v.getBateria() : 100.0,
                        municipio
                    );
                    vs.setDistanciaRecorrida(v.getDistanciaTotal() != null ? v.getDistanciaTotal() : 0.0);
                    vs.setKmh(0.0);
                    vehiculos.add(vs);
                }
            } catch (Exception ignored) {}
        }

        return vehiculos;
    }
}

