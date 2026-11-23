package edu.uclm.esi.tysweb.bealquileres.http;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.esi.tysweb.bealquileres.dao.EstacionDao;
import edu.uclm.esi.tysweb.bealquileres.dto.AlquilarRequestDto;
import edu.uclm.esi.tysweb.bealquileres.dto.AlquilerResponseDto;
import edu.uclm.esi.tysweb.bealquileres.dto.BicicletaDisponibleDto;
import edu.uclm.esi.tysweb.bealquileres.dto.DejarRequestDto;
import edu.uclm.esi.tysweb.bealquileres.dto.UsuarioAutenticadoDto;
import edu.uclm.esi.tysweb.bealquileres.model.Estacion;
import edu.uclm.esi.tysweb.bealquileres.services.AlquilerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/alquileres")
@CrossOrigin("*")
@Tag(name = "Alquileres", description = "Operaciones de alquiler de bicicletas para ciudadanos")
public class AlquilerController {

    @Autowired
    private AlquilerService alquilerService;

    @Autowired
    private EstacionDao estacionDao;

    @PostMapping("/alquilar")
    @Operation(summary = "Alquila una bicicleta para el ciudadano autenticado (RN-ALQ-01, RN-ALQ-02, RC-CON-01)")
    public AlquilerResponseDto alquilar(@RequestBody AlquilarRequestDto request, HttpServletRequest httpRequest) {
        UsuarioAutenticadoDto usuario = (UsuarioAutenticadoDto) httpRequest.getAttribute("usuarioAutenticado");
        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }
        return this.alquilerService.alquilar(usuario, request);
    }

    @PostMapping("/dejar")
    @Operation(summary = "Registra la devolución de una bicicleta (RN-ALQ-03, invocado automáticamente por simulador o usuario)")
    public AlquilerResponseDto dejar(@RequestBody DejarRequestDto request) {
        return this.alquilerService.dejar(request);
    }

    @GetMapping("/activo")
    @Operation(summary = "Devuelve el alquiler actualmente activo del usuario autenticado")
    public AlquilerResponseDto getAlquilerActivo(HttpServletRequest httpRequest) {
        UsuarioAutenticadoDto usuario = (UsuarioAutenticadoDto) httpRequest.getAttribute("usuarioAutenticado");
        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }
        return this.alquilerService.getAlquilerActivo(usuario.email())
                .orElse(null);
    }

    @GetMapping("/historial")
    @Operation(summary = "Devuelve el historial de alquileres finalizados del usuario autenticado")
    public List<AlquilerResponseDto> getHistorial(HttpServletRequest httpRequest) {
        UsuarioAutenticadoDto usuario = (UsuarioAutenticadoDto) httpRequest.getAttribute("usuarioAutenticado");
        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }
        return this.alquilerService.getHistorial(usuario.email());
    }

    @GetMapping("/estacion/{estacionId}/disponibles")
    @Operation(summary = "Devuelve las bicicletas activas y disponibles en una estación con su % de batería")
    public List<BicicletaDisponibleDto> getBicisDisponibles(@PathVariable Long estacionId) {
        return this.alquilerService.getBicisDisponiblesEnEstacion(estacionId);
    }

    @GetMapping("/estaciones")
    @Operation(summary = "Devuelve las estaciones con número de bicis disponibles y distancia al usuario (RT-LOC-03, RT-LOC-04)")
    public List<Map<String, Object>> getEstacionesConDisponibilidad(
            @RequestParam(required = false, defaultValue = "Ciudad Real") String municipio,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon) {

        List<Estacion> estaciones = this.estacionDao.findByMunicipioName(municipio);
        List<Map<String, Object>> resultado = new ArrayList<>();

        for (Estacion e : estaciones) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", e.getId());
            map.put("name", e.getName());
            map.put("number", e.getNumber());
            map.put("latitude", e.getLatitude());
            map.put("longitude", e.getLongitude());
            map.put("capacidad", e.getCapacidad() != null ? e.getCapacidad() : 10);

            List<BicicletaDisponibleDto> bicis = this.alquilerService.getBicisDisponiblesEnEstacion(e.getId());
            map.put("bicicletasDisponibles", bicis.size());
            map.put("bicicletas", bicis);

            if (lat != null && lon != null && e.getLatitude() != null && e.getLongitude() != null) {
                double distanciaMetros = calcularDistanciaHaversine(lat, lon, e.getLatitude(), e.getLongitude());
                map.put("distanciaMetros", Math.round(distanciaMetros));
            } else {
                map.put("distanciaMetros", null);
            }

            resultado.add(map);
        }

        if (lat != null && lon != null) {
            resultado.sort(Comparator.comparingDouble(m -> {
                Object dist = m.get("distanciaMetros");
                return dist != null ? ((Number) dist).doubleValue() : Double.MAX_VALUE;
            }));
        }

        return resultado;
    }

    private double calcularDistanciaHaversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Radio de la Tierra en metros
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
