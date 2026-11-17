package edu.uclm.esi.tysweb.beusu.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.esi.tysweb.beusu.dao.CompraDao;
import edu.uclm.esi.tysweb.beusu.dao.UserDao;
import edu.uclm.esi.tysweb.beusu.model.Compra;
import edu.uclm.esi.tysweb.beusu.model.User;

@ExtendWith(MockitoExtension.class)
public class PaymentsServiceTest {

    @Mock
    private CompraDao compraDao;

    @Mock
    private UserDao userDao;

    private PaymentsService paymentsService;

    private User empleadoAyto;
    private User ciudadano;

    @BeforeEach
    void setUp() {
        paymentsService = new PaymentsService(
                compraDao,
                userDao,
                "mock_secret_key_for_dev",
                "http://localhost:8081"
        );

        empleadoAyto = new User();
        empleadoAyto.setId("emp-1");
        empleadoAyto.setEmail("empleado@ayto.es");
        empleadoAyto.setNombre("Carlos");
        empleadoAyto.setMunicipio("Ciudad Real");
        empleadoAyto.setRol("EMPLEADO_AYTO");
        empleadoAyto.setActivo(true);

        ciudadano = new User();
        ciudadano.setId("ciu-1");
        ciudadano.setEmail("ciudadano@gmail.com");
        ciudadano.setNombre("Ana");
        ciudadano.setRol("CIUDADANO");
        ciudadano.setActivo(true);
        ciudadano.setPagadoAlta(false);
    }

    // -------------------------------------------------------------------------
    // PASO 2.3: prepay
    // -------------------------------------------------------------------------
    @Test
    void testPrepay_UsuarioNoExiste_LanzaNotFound404() {
        when(userDao.findByEmail("desconocido@ayto.es")).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                paymentsService.prepay("desconocido@ayto.es", 6000.0, "BICICLETAS", 5, "Ciudad Real")
        );
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void testPrepay_ImporteInvalido_LanzaBadRequest400() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                paymentsService.prepay("empleado@ayto.es", -10.0, "BICICLETAS", 1, "Ciudad Real")
        );
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testPrepay_BicicletasExitoso_CreaCompraYDevuelveIdTransaccion() {
        when(userDao.findByEmail("empleado@ayto.es")).thenReturn(empleadoAyto);

        String idTransaccion = paymentsService.prepay("empleado@ayto.es", 6000.0, "BICICLETAS", 5, "Ciudad Real");

        assertNotNull(idTransaccion);
        assertTrue(idTransaccion.contains("secret_"));
        verify(compraDao).save(argThat(compra ->
                compra.getUser().getEmail().equals("empleado@ayto.es") &&
                compra.getImporte().equals(6000.0) &&
                compra.getCantidad() == 5 &&
                "Ciudad Real".equals(compra.getMunicipio()) &&
                !compra.isPagado()
        ));
    }

    @Test
    void testPrepay_AltaCiudadano_Importe100Euros_RN_CIU_02() {
        when(userDao.findByEmail("ciudadano@gmail.com")).thenReturn(ciudadano);

        String idTransaccion = paymentsService.prepay("ciudadano@gmail.com", 100.0, "ALTA_CIUDADANO", 1, null);

        assertNotNull(idTransaccion);
        verify(compraDao).save(argThat(compra ->
                compra.getUser().getEmail().equals("ciudadano@gmail.com") &&
                compra.getImporte().equals(100.0) &&
                "ALTA_CIUDADANO".equals(compra.getTipo()) &&
                !compra.isPagado()
        ));
    }

    // -------------------------------------------------------------------------
    // PASO 4.3: confirm (validación anti-fraude y consolidación)
    // -------------------------------------------------------------------------
    @Test
    void testConfirm_TransaccionNoExiste_LanzaNotFound404() {
        when(compraDao.findByIdTransaccion("pi_inexistente")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                paymentsService.confirm("pi_inexistente")
        );
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void testConfirm_AltaCiudadano_ActivaPagadoAlta_RN_CIU_02() {
        Compra compra = new Compra(ciudadano, 100.0, "ALTA_CIUDADANO", 1, null);
        compra.setIdTransaccion("pi_mock_123_secret_456");
        compra.setPagado(false);

        when(compraDao.findByIdTransaccion("pi_mock_123_secret_456")).thenReturn(Optional.of(compra));

        Map<String, String> res = paymentsService.confirm("pi_mock_123_secret_456");

        assertEquals("Pago realizado correctamente", res.get("message"));
        assertTrue(compra.isPagado());
        assertTrue(ciudadano.isPagadoAlta(), "El ciudadano debe quedar con pagadoAlta=true tras confirmar el pago (RN-CIU-02)");
        verify(compraDao).save(compra);
        verify(userDao).save(ciudadano);
    }

    @Test
    void testConfirm_CompraBicicletas_MarcaComoPagada() {
        Compra compra = new Compra(empleadoAyto, 6000.0, "BICICLETAS", 5, "Ciudad Real");
        compra.setIdTransaccion("pi_mock_bici_secret_789");
        compra.setPagado(false);

        when(compraDao.findByIdTransaccion("pi_mock_bici_secret_789")).thenReturn(Optional.of(compra));

        Map<String, String> res = paymentsService.confirm("pi_mock_bici_secret_789");

        assertEquals("Pago realizado correctamente", res.get("message"));
        assertTrue(compra.isPagado());
        verify(compraDao).save(compra);
    }
}
