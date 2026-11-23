package edu.uclm.esi.tysweb.bealquileres.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.esi.tysweb.bealquileres.dao.AlquilerDao;
import edu.uclm.esi.tysweb.bealquileres.dao.EstacionDao;
import edu.uclm.esi.tysweb.bealquileres.dao.VehiculoDao;
import edu.uclm.esi.tysweb.bealquileres.dto.AlquilarRequestDto;
import edu.uclm.esi.tysweb.bealquileres.dto.AlquilerResponseDto;
import edu.uclm.esi.tysweb.bealquileres.dto.DejarRequestDto;
import edu.uclm.esi.tysweb.bealquileres.dto.UsuarioAutenticadoDto;
import edu.uclm.esi.tysweb.bealquileres.model.Alquiler;
import edu.uclm.esi.tysweb.bealquileres.model.Estacion;
import edu.uclm.esi.tysweb.bealquileres.model.Municipio;
import edu.uclm.esi.tysweb.bealquileres.model.Vehiculo;

@ExtendWith(MockitoExtension.class)
public class AlquilerServiceTest {

    @Mock
    private AlquilerDao alquilerDao;

    @Mock
    private VehiculoDao vehiculoDao;

    @Mock
    private EstacionDao estacionDao;

    @Mock
    private SimuladorClient simuladorClient;

    @InjectMocks
    private AlquilerService alquilerService;

    private UsuarioAutenticadoDto ciudadanoConAlta;
    private UsuarioAutenticadoDto ciudadanoSinAlta;
    private Vehiculo vehiculo;
    private Estacion estacionOrigen;
    private Estacion estacionDestino;
    private Municipio municipio;

    @BeforeEach
    void setUp() {
        ciudadanoConAlta = new UsuarioAutenticadoDto("u1", "juan@ciudadano.es", "Juan", "CIUDADANO", "Ciudad Real", true);
        ciudadanoSinAlta = new UsuarioAutenticadoDto("u2", "pedro@ciudadano.es", "Pedro", "CIUDADANO", "Ciudad Real", false);

        municipio = new Municipio();
        municipio.setName("Ciudad Real");

        estacionOrigen = new Estacion();
        estacionOrigen.setId(1L);
        estacionOrigen.setName("Estación Universidad");
        estacionOrigen.setLatitude(38.9861);
        estacionOrigen.setLongitude(-3.9273);
        estacionOrigen.setMunicipio(municipio);

        estacionDestino = new Estacion();
        estacionDestino.setId(2L);
        estacionDestino.setName("Estación Plaza Mayor");
        estacionDestino.setLatitude(38.9855);
        estacionDestino.setLongitude(-3.9280);
        estacionDestino.setMunicipio(municipio);

        vehiculo = new Vehiculo();
        vehiculo.setId(10L);
        vehiculo.setBateria(100.0);
        vehiculo.setActivo(true);
        vehiculo.setEstacion(estacionOrigen);
        vehiculo.setMunicipio(municipio);
    }

    @Test
    void testRechazoAlquilerSinPagoAlta_RN_CIU_02() {
        AlquilarRequestDto req = new AlquilarRequestDto(10L, 2L);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            alquilerService.alquilar(ciudadanoSinAlta, req)
        );
        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("100 €"));
    }

    @Test
    void testRechazoAlquilerConAlquilerActivoPrevio_RN_ALQ_01() {
        AlquilarRequestDto req = new AlquilarRequestDto(10L, 2L);
        when(alquilerDao.findByUsuarioEmailAndActivoTrue(ciudadanoConAlta.email()))
                .thenReturn(Optional.of(new Alquiler()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            alquilerService.alquilar(ciudadanoConAlta, req)
        );
        assertEquals(409, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("alquiler activo"));
    }

    @Test
    void testRechazoAlquilerBicicletaInactiva_RN_BIC_04() {
        vehiculo.setActivo(false);
        vehiculo.setObservaciones("pinchada");

        AlquilarRequestDto req = new AlquilarRequestDto(10L, 2L);
        when(alquilerDao.findByUsuarioEmailAndActivoTrue(ciudadanoConAlta.email())).thenReturn(Optional.empty());
        when(vehiculoDao.findById(10L)).thenReturn(Optional.of(vehiculo));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            alquilerService.alquilar(ciudadanoConAlta, req)
        );
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("pinchada"));
    }

    @Test
    void testAlquilarExitoso_RC_CON_01() {
        AlquilarRequestDto req = new AlquilarRequestDto(10L, 2L);
        when(alquilerDao.findByUsuarioEmailAndActivoTrue(ciudadanoConAlta.email())).thenReturn(Optional.empty());
        when(vehiculoDao.findById(10L)).thenReturn(Optional.of(vehiculo));
        when(alquilerDao.findByVehiculoIdAndActivoTrue(10L)).thenReturn(Optional.empty());
        when(estacionDao.findById(2L)).thenReturn(Optional.of(estacionDestino));
        when(alquilerDao.save(any(Alquiler.class))).thenAnswer(invocation -> {
            Alquiler a = invocation.getArgument(0);
            a.setId(99L);
            return a;
        });

        AlquilerResponseDto dto = alquilerService.alquilar(ciudadanoConAlta, req);

        assertNotNull(dto);
        assertEquals(99L, dto.id());
        assertEquals("juan@ciudadano.es", dto.usuarioEmail());
        assertNull(vehiculo.getEstacion(), "La bicicleta debe haber salido de la estación origen");

        verify(simuladorClient, times(1)).solicitarMoverVehiculo(
                eq(10L), anyString(), anyString(),
                anyDouble(), anyDouble(), anyString(),
                anyDouble(), anyDouble(), anyString(),
                eq(2L), anyDouble()
        );
    }

    @Test
    void testDejarBicicleta_RN_ALQ_03() {
        Alquiler alquilerActivo = new Alquiler("juan@ciudadano.es", vehiculo, estacionOrigen, estacionDestino);
        alquilerActivo.setId(50L);

        when(alquilerDao.findByVehiculoIdAndActivoTrue(10L)).thenReturn(Optional.of(alquilerActivo));
        when(estacionDao.findById(2L)).thenReturn(Optional.of(estacionDestino));
        when(alquilerDao.save(any(Alquiler.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DejarRequestDto dejarReq = new DejarRequestDto(10L, "0010", 38.9855, -3.9280, 2.5, 95.0, 2L);
        AlquilerResponseDto res = alquilerService.dejar(dejarReq);

        assertNotNull(res);
        assertFalse(alquilerActivo.getActivo(), "El alquiler debe quedar cerrado");
        assertNotNull(alquilerActivo.getFechaFin());
        assertEquals(estacionDestino, vehiculo.getEstacion(), "El vehículo debe quedar en la estación destino");
        assertEquals(95.0, vehiculo.getBateria());
    }

    @Test
    void testRecargaBateriaEnEstacion_RN_BIC_03() {
        vehiculo.setBateria(50.0);
        // Simular que entró en la estación hace 2 horas (debería haber cargado 2h * 25% = 50% adicional -> 100%)
        vehiculo.setHoraEntradaEstacion(LocalDateTime.now().minusHours(2));

        Double batCalculada = vehiculo.getBateriaCalculada();
        assertEquals(100.0, batCalculada, 0.5);
    }
}
