package edu.uclm.esi.tysweb.bealquileres.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.esi.tysweb.bealquileres.dao.AlquilerDao;
import edu.uclm.esi.tysweb.bealquileres.dao.EstacionDao;
import edu.uclm.esi.tysweb.bealquileres.dao.VehiculoDao;
import edu.uclm.esi.tysweb.bealquileres.dto.AlquilarRequestDto;
import edu.uclm.esi.tysweb.bealquileres.dto.AlquilerResponseDto;
import edu.uclm.esi.tysweb.bealquileres.dto.BicicletaDisponibleDto;
import edu.uclm.esi.tysweb.bealquileres.dto.DejarRequestDto;
import edu.uclm.esi.tysweb.bealquileres.dto.UsuarioAutenticadoDto;
import edu.uclm.esi.tysweb.bealquileres.model.Alquiler;
import edu.uclm.esi.tysweb.bealquileres.model.Estacion;
import edu.uclm.esi.tysweb.bealquileres.model.Vehiculo;

@Service
public class AlquilerService {

    @Autowired
    private AlquilerDao alquilerDao;

    @Autowired
    private VehiculoDao vehiculoDao;

    @Autowired
    private EstacionDao estacionDao;

    @Autowired
    private SimuladorClient simuladorClient;

    private final Object lock = new Object();

    /**
     * Proceso de Alquiler de bicicleta para un ciudadano (RN-ALQ-01, RN-ALQ-02, RC-CON-01, RN-CIU-02).
     * Sincronizado para evitar condiciones de carrera si dos usuarios solicitan la misma bicicleta simultáneamente.
     */
    public AlquilerResponseDto alquilar(UsuarioAutenticadoDto usuario, AlquilarRequestDto req) {
        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }

        // RN-CIU-02 / RN-ALQ-01: Exigir cuota de alta de 100 € abonada
        if (!usuario.pagadoAlta()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                "Debe abonar la fianza / cuota de alta de 100 € para poder alquilar bicicletas (RN-CIU-02)");
        }

        // RN-ALQ-01(c): Comprobar que no tenga un alquiler activo en curso
        if (this.alquilerDao.findByUsuarioEmailAndActivoTrue(usuario.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, 
                "Ya tiene un alquiler activo en curso. Debe devolver la bicicleta antes de alquilar otra (RN-ALQ-01)");
        }

        if (req == null || req.getVehiculoId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe especificar el identificador de la bicicleta a alquilar");
        }

        synchronized (this.lock) {
            // Verificar existencia y disponibilidad de la bicicleta
            Vehiculo vehiculo = this.vehiculoDao.findById(req.getVehiculoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bicicleta no encontrada"));

            // RN-BIC-04: Verificar que la bicicleta esté activa y en estado operativo
            if (Boolean.FALSE.equals(vehiculo.getActivo())) {
                String motivo = vehiculo.getObservaciones() != null ? vehiculo.getObservaciones() : "inactiva";
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La bicicleta no está disponible para su uso (" + motivo + ")");
            }

            // Debe encontrarse ubicada en una estación
            if (vehiculo.getEstacion() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La bicicleta seleccionada no se encuentra en ninguna estación disponible");
            }

            // Verificar que ningún otro usuario la haya alquilado
            if (this.alquilerDao.findByVehiculoIdAndActivoTrue(vehiculo.getId()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La bicicleta seleccionada acaba de ser alquilada por otro usuario (RC-CON-01)");
            }

            Estacion estacionOrigen = vehiculo.getEstacion();

            // Determinar estación destino en el mismo municipio
            Estacion estacionDestino = null;
            if (req.getEstacionDestinoId() != null) {
                estacionDestino = this.estacionDao.findById(req.getEstacionDestinoId()).orElse(null);
            }

            if (estacionDestino == null || (estacionDestino.getId().equals(estacionOrigen.getId()))) {
                if (estacionOrigen.getMunicipio() != null) {
                    List<Estacion> estaciones = this.estacionDao.findByMunicipioId(estacionOrigen.getMunicipio().getId());
                    for (Estacion e : estaciones) {
                        if (!e.getId().equals(estacionOrigen.getId())) {
                            estacionDestino = e;
                            break;
                        }
                    }
                }
                if (estacionDestino == null) {
                    estacionDestino = estacionOrigen;
                }
            }

            // Sincronizar y consolidar la batería calculada con la recarga en estación (RN-BIC-03)
            Double bateriaActual = vehiculo.getBateriaCalculada();
            vehiculo.setBateria(bateriaActual != null ? bateriaActual : 100.0);

            // Desvincular de la estación origen para iniciar el trayecto
            vehiculo.setEstacion(null);
            vehiculo.setHoraEntradaEstacion(null);
            this.vehiculoDao.save(vehiculo);

            // Registrar el alquiler
            Alquiler alquiler = new Alquiler(usuario.email(), vehiculo, estacionOrigen, estacionDestino);
            alquiler = this.alquilerDao.save(alquiler);

            // Iniciar la simulación del recorrido hacia la estación de destino
            String municipioNombre = (estacionOrigen.getMunicipio() != null && estacionOrigen.getMunicipio().getName() != null)
                    ? estacionOrigen.getMunicipio().getName()
                    : "Ciudad Real";

            this.simuladorClient.solicitarMoverVehiculo(
                    vehiculo.getId(),
                    vehiculo.getMatricula(),
                    municipioNombre,
                    estacionOrigen.getLatitude(),
                    estacionOrigen.getLongitude(),
                    estacionOrigen.getName(),
                    estacionDestino.getLatitude(),
                    estacionDestino.getLongitude(),
                    estacionDestino.getName(),
                    estacionDestino.getId(),
                    vehiculo.getBateria()
            );

            return toDto(alquiler);
        }
    }

    /**
     * Endpoint invocado automáticamente por el simulador al llegar a destino (RN-ALQ-03)
     * o por un usuario al devolver la bicicleta.
     */
    @Transactional
    public synchronized AlquilerResponseDto dejar(DejarRequestDto req) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos de devolución requeridos");
        }

        Alquiler alquiler = null;
        if (req.getVehiculoId() != null) {
            alquiler = this.alquilerDao.findByVehiculoIdAndActivoTrue(req.getVehiculoId()).orElse(null);
        }

        if (alquiler == null && req.getMatricula() != null) {
            try {
                Long vId = Long.parseLong(req.getMatricula());
                alquiler = this.alquilerDao.findByVehiculoIdAndActivoTrue(vId).orElse(null);
            } catch (Exception ignored) {}
        }

        Vehiculo vehiculo = null;
        if (alquiler != null) {
            vehiculo = alquiler.getVehiculo();
        } else if (req.getVehiculoId() != null) {
            vehiculo = this.vehiculoDao.findById(req.getVehiculoId()).orElse(null);
        }

        if (vehiculo == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehículo no encontrado");
        }

        Estacion estacionDestino = null;
        if (req.getEstacionDestinoId() != null) {
            estacionDestino = this.estacionDao.findById(req.getEstacionDestinoId()).orElse(null);
        }
        if (estacionDestino == null && alquiler != null && alquiler.getEstacionDestino() != null) {
            estacionDestino = alquiler.getEstacionDestino();
        }

        // Ubicar la bicicleta en la estación de llegada
        if (estacionDestino != null) {
            vehiculo.setEstacion(estacionDestino); // actualiza horaEntradaEstacion = LocalDateTime.now()
            vehiculo.setLatitud(estacionDestino.getLatitude());
            vehiculo.setLongitud(estacionDestino.getLongitude());
        } else if (req.getLatitud() != null && req.getLongitud() != null) {
            vehiculo.setLatitud(req.getLatitud());
            vehiculo.setLongitud(req.getLongitud());
        }

        if (req.getBateria() != null) {
            vehiculo.setBateria(Math.max(0.0, Math.min(100.0, req.getBateria())));
        }
        if (req.getDistanciaRecorrida() != null) {
            double distAnterior = vehiculo.getDistanciaTotal() != null ? vehiculo.getDistanciaTotal() : 0.0;
            vehiculo.setDistanciaTotal(distAnterior + req.getDistanciaRecorrida());
        }
        this.vehiculoDao.save(vehiculo);

        if (alquiler != null) {
            alquiler.setActivo(false);
            alquiler.setFechaFin(LocalDateTime.now());
            if (req.getDistanciaRecorrida() != null) {
                alquiler.setDistanciaRecorrida(req.getDistanciaRecorrida());
            }
            if (estacionDestino != null) {
                alquiler.setEstacionDestino(estacionDestino);
            }
            alquiler = this.alquilerDao.save(alquiler);
            return toDto(alquiler);
        }

        return new AlquilerResponseDto(
                null,
                null,
                vehiculo.getId(),
                vehiculo.getMatricula(),
                null,
                null,
                estacionDestino != null ? estacionDestino.getId() : null,
                estacionDestino != null ? estacionDestino.getName() : null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                false,
                req.getDistanciaRecorrida() != null ? req.getDistanciaRecorrida() : 0.0,
                vehiculo.getBateria()
        );
    }

    public Optional<AlquilerResponseDto> getAlquilerActivo(String usuarioEmail) {
        return this.alquilerDao.findByUsuarioEmailAndActivoTrue(usuarioEmail)
                .map(this::toDto);
    }

    public List<AlquilerResponseDto> getHistorial(String usuarioEmail) {
        return this.alquilerDao.findByUsuarioEmailOrderByFechaInicioDesc(usuarioEmail).stream()
                .map(this::toDto)
                .toList();
    }

    public List<BicicletaDisponibleDto> getBicisDisponiblesEnEstacion(Long estacionId) {
        List<Vehiculo> vehiculos = this.vehiculoDao.findByEstacionIdAndActivoTrue(estacionId);
        List<BicicletaDisponibleDto> result = new ArrayList<>();

        for (Vehiculo v : vehiculos) {
            // No mostrar bicicletas que tengan alquiler activo
            if (this.alquilerDao.findByVehiculoIdAndActivoTrue(v.getId()).isPresent()) {
                continue;
            }

            Double bat = v.getBateriaCalculada();
            String estNombre = v.getEstacion() != null ? v.getEstacion().getName() : "Estación";

            result.add(new BicicletaDisponibleDto(
                    v.getId(),
                    v.getMatricula(),
                    v.getLatitud(),
                    v.getLongitud(),
                    bat != null ? Math.round(bat * 10.0) / 10.0 : 100.0,
                    estacionId,
                    estNombre,
                    v.getActivo(),
                    v.getObservaciones()
            ));
        }

        return result;
    }

    private AlquilerResponseDto toDto(Alquiler a) {
        Vehiculo v = a.getVehiculo();
        Double bateria = v != null ? (v.getEstacion() != null ? v.getBateriaCalculada() : v.getBateria()) : 100.0;
        return new AlquilerResponseDto(
                a.getId(),
                a.getUsuarioEmail(),
                v != null ? v.getId() : null,
                v != null ? v.getMatricula() : null,
                a.getEstacionOrigen() != null ? a.getEstacionOrigen().getId() : null,
                a.getEstacionOrigen() != null ? a.getEstacionOrigen().getName() : null,
                a.getEstacionDestino() != null ? a.getEstacionDestino().getId() : null,
                a.getEstacionDestino() != null ? a.getEstacionDestino().getName() : null,
                a.getFechaInicio(),
                a.getFechaFin(),
                a.getActivo(),
                a.getDistanciaRecorrida(),
                bateria != null ? Math.round(bateria * 10.0) / 10.0 : 100.0
        );
    }
}
