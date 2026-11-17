package edu.uclm.esi.tysweb.bealquileres.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.esi.tysweb.bealquileres.dao.EstacionDao;
import edu.uclm.esi.tysweb.bealquileres.dao.MunicipioDao;
import edu.uclm.esi.tysweb.bealquileres.dao.VehiculoDao;
import edu.uclm.esi.tysweb.bealquileres.dto.VehiculoDto;
import edu.uclm.esi.tysweb.bealquileres.model.Municipio;
import edu.uclm.esi.tysweb.bealquileres.model.Vehiculo;

@ExtendWith(MockitoExtension.class)
public class PrerreservaYCompraTest {

    @Mock
    private VehiculoDao vehiculoDao;

    @Mock
    private MunicipioDao municipioDao;

    @Mock
    private EstacionDao estacionDao;

    @InjectMocks
    private VehiculoService vehiculoService;

    private Municipio municipioTest;

    @BeforeEach
    void setUp() {
        municipioTest = new Municipio();
        municipioTest.setId("mun-1");
        municipioTest.setName("Ciudad Real");
    }

    // -------------------------------------------------------------------------
    // RN-COM-04: Precio de referencia (1200 euros)
    // -------------------------------------------------------------------------
    @Test
    void testGetPrecioDeBicicleta_Retorna1200Euros() {
        int precio = vehiculoService.getPrecioDeBicicleta();
        assertEquals(1200, precio, "El precio de cada bicicleta debe ser exactamente 1200 euros según RN-COM-04");
    }

    // -------------------------------------------------------------------------
    // RN-COM-01 & RN-COM-02: Prerreserva de bicicletas
    // -------------------------------------------------------------------------
    @Test
    void testPrerreservar_MunicipioNoExiste_LanzaNotFound404() {
        when(municipioDao.findByName("Desconocido")).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                vehiculoService.prerreservar("Desconocido", 5)
        );
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void testPrerreservar_NoHayBicicletasSuficientes_LanzaConflict409() {
        when(municipioDao.findByName("Ciudad Real")).thenReturn(municipioTest);
        when(vehiculoDao.findDisponiblesParaPrerreserva(any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                vehiculoService.prerreservar("Ciudad Real", 3)
        );
        assertEquals(409, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("No hay suficientes"));
    }

    @Test
    void testPrerreservar_Exitoso_AsignaMunicipioYHoraDePrerreserva() {
        when(municipioDao.findByName("Ciudad Real")).thenReturn(municipioTest);

        List<Vehiculo> disponibles = new ArrayList<>();
        for (long i = 1; i <= 3; i++) {
            Vehiculo v = new Vehiculo();
            v.setId(i);
            disponibles.add(v);
        }
        when(vehiculoDao.findDisponiblesParaPrerreserva(any(LocalDateTime.class))).thenReturn(disponibles);

        List<VehiculoDto> res = vehiculoService.prerreservar("Ciudad Real", 3);

        assertEquals(3, res.size());
        verify(vehiculoDao, times(3)).save(any(Vehiculo.class));

        for (Vehiculo v : disponibles) {
            assertEquals("Ciudad Real", v.getMunicipio().getName());
            assertNotNull(v.getHoraDePrerreserva(), "Debe establecerse la horaDePrerreserva (RN-COM-02)");
        }
    }

    // -------------------------------------------------------------------------
    // RN-COM-03: Confirmación definitiva tras el pago
    // -------------------------------------------------------------------------
    @Test
    void testConfirmarCompra_EliminaHoraPrerreservaHaciendolaDefinitiva() {
        List<Vehiculo> preasignadas = new ArrayList<>();
        for (long i = 1; i <= 2; i++) {
            Vehiculo v = new Vehiculo();
            v.setId(i);
            v.setMunicipio(municipioTest);
            v.setHoraDePrerreserva(LocalDateTime.now().minusMinutes(5));
            preasignadas.add(v);
        }

        when(vehiculoDao.findByMunicipioNameAndHoraDePrerreservaIsNotNull("Ciudad Real")).thenReturn(preasignadas);

        vehiculoService.confirmarCompra("Ciudad Real", 2);

        verify(vehiculoDao, times(2)).save(any(Vehiculo.class));
        for (Vehiculo v : preasignadas) {
            assertNull(v.getHoraDePrerreserva(), "La hora de prerreserva debe pasar a null al confirmar definitivamente (RN-COM-03)");
        }
    }

    // -------------------------------------------------------------------------
    // RC-CON-03: Concurrencia en prerreserva sincronizada
    // -------------------------------------------------------------------------
    @Test
    void testPrerreservaConcurrente_RC_CON_03() throws InterruptedException {
        when(municipioDao.findByName("Ciudad Real")).thenReturn(municipioTest);

        List<Vehiculo> stock = Collections.synchronizedList(new ArrayList<>());
        for (long i = 1; i <= 5; i++) {
            Vehiculo v = new Vehiculo();
            v.setId(i);
            stock.add(v);
        }

        // Mock dinámico: cada vez que se consulte, devuelve el stock restante
        when(vehiculoDao.findDisponiblesParaPrerreserva(any())).thenAnswer(inv -> new ArrayList<>(stock));
        doAnswer(inv -> {
            Vehiculo v = inv.getArgument(0);
            stock.remove(v);
            return v;
        }).when(vehiculoDao).save(any(Vehiculo.class));

        int numHilos = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numHilos);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger exitos = new AtomicInteger(0);
        AtomicInteger conflictos = new AtomicInteger(0);

        for (int i = 0; i < numHilos; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    // Ambos hilos intentan prerreservar 5 vehículos a la vez
                    vehiculoService.prerreservar("Ciudad Real", 5);
                    exitos.incrementAndGet();
                } catch (ResponseStatusException rse) {
                    if (rse.getStatusCode().value() == 409) {
                        conflictos.incrementAndGet();
                    }
                } catch (Exception ignored) {
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(3, java.util.concurrent.TimeUnit.SECONDS);

        // Debido a synchronized, exactamente 1 hilo debe haber tenido éxito y el otro debe haber recibido 409 CONFLICT
        assertEquals(1, exitos.get(), "Exactamente una de las dos compras concurrentes debe tener éxito");
        assertEquals(1, conflictos.get(), "La otra compra concurrente debe recibir 409 Conflict al agotarse el stock");
    }
}
