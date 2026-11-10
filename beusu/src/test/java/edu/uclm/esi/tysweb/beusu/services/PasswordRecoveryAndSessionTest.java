package edu.uclm.esi.tysweb.beusu.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.esi.tysweb.beusu.JwtUtil;
import edu.uclm.esi.tysweb.beusu.dao.UserDao;
import edu.uclm.esi.tysweb.beusu.dto.ResetPasswordRequest;
import edu.uclm.esi.tysweb.beusu.dto.UsuarioValidadoDto;
import edu.uclm.esi.tysweb.beusu.model.User;

@ExtendWith(MockitoExtension.class)
public class PasswordRecoveryAndSessionTest {

    @Mock
    private UserDao userDao;

    @Mock
    private EmailService emailService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private IdeeClient ideeClient;

    @Mock
    private GoogleAuthClient googleAuth;

    @InjectMocks
    private UsersService usersService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(usersService, "tokenConfirmacionExpirationMs", 86400000L); // 24h
        ReflectionTestUtils.setField(usersService, "tokenRecuperacionExpirationMs", 1800000L);  // 30min
    }

    // -------------------------------------------------------------------------
    // RC-SEC-03: Mensaje neutro en solicitud de recuperación
    // -------------------------------------------------------------------------
    @Test
    void testSolicitarRecuperacion_UsuarioInexistente_DevuelveMensajeNeutro() {
        when(userDao.findByEmail("noexiste@ayto.es")).thenReturn(null);

        Map<String, String> response = usersService.solicitarRecuperacionPwd("noexiste@ayto.es");

        assertNotNull(response.get("message"));
        assertTrue(response.get("message").contains("Si el correo electrónico está registrado"));
        assertNull(response.get("token")); // No se emite token si no existe
        verify(emailService, never()).enviarCorreoRecuperacion(any(), any());
    }

    @Test
    void testSolicitarRecuperacion_UsuarioExistenteActivo_GeneraTokenYDevuelveMensajeNeutro() {
        User user = new User();
        user.setEmail("carlos@ayto.es");
        user.setActivo(true);
        when(userDao.findByEmail("carlos@ayto.es")).thenReturn(user);

        Map<String, String> response = usersService.solicitarRecuperacionPwd("carlos@ayto.es");

        assertNotNull(response.get("message"));
        assertTrue(response.get("message").contains("Si el correo electrónico está registrado"));
        assertNotNull(user.getTokenRecuperacion());
        assertNotNull(user.getHoraRecuperacion());
        verify(userDao).save(user);
        verify(emailService).enviarCorreoRecuperacion(eq("carlos@ayto.es"), eq(user.getTokenRecuperacion()));
    }

    // -------------------------------------------------------------------------
    // RN-CUE-04: Si ya había una solicitud previa, se invalida la anterior
    // -------------------------------------------------------------------------
    @Test
    void testSolicitarRecuperacion_InvalidaTokenPrevio() {
        User user = new User();
        user.setEmail("carlos@ayto.es");
        user.setActivo(true);
        user.setTokenRecuperacion("token-antiguo-123");
        user.setHoraRecuperacion(System.currentTimeMillis() - 5000);
        when(userDao.findByEmail("carlos@ayto.es")).thenReturn(user);

        usersService.solicitarRecuperacionPwd("carlos@ayto.es");

        assertNotEquals("token-antiguo-123", user.getTokenRecuperacion());
        verify(userDao).save(user);
    }

    // -------------------------------------------------------------------------
    // RN-CUE-04 & RT-CONF-02: Creación de nueva contraseña con token
    // -------------------------------------------------------------------------
    @Test
    void testCambiarPassword_TokenInexistente_LanzaNotFound() {
        when(userDao.findByTokenRecuperacion("token-inexistente")).thenReturn(null);

        ResetPasswordRequest req = new ResetPasswordRequest("token-inexistente", "NuevaPwd123!", "NuevaPwd123!");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                usersService.cambiarPasswordConToken(req)
        );
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void testCambiarPassword_TokenExpirado_LanzaGone410() {
        User user = new User();
        user.setEmail("carlos@ayto.es");
        user.setActivo(true);
        user.setTokenRecuperacion("token-expirado");
        // Caducó hace 40 minutos (el límite es 30 min)
        user.setHoraRecuperacion(System.currentTimeMillis() - (40 * 60 * 1000L));
        when(userDao.findByTokenRecuperacion("token-expirado")).thenReturn(user);

        ResetPasswordRequest req = new ResetPasswordRequest("token-expirado", "NuevaPwd123!", "NuevaPwd123!");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                usersService.cambiarPasswordConToken(req)
        );
        assertEquals(410, ex.getStatusCode().value());
    }

    @Test
    void testCambiarPassword_PasswordsNoCoinciden_LanzaBadRequest() {
        ResetPasswordRequest req = new ResetPasswordRequest("token-valido", "Password123!", "PasswordDistinta123!");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                usersService.cambiarPasswordConToken(req)
        );
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testCambiarPassword_Exitoso_ActualizaPasswordEInvalidaToken() {
        User user = new User();
        user.setEmail("carlos@ayto.es");
        user.setActivo(true);
        user.setTokenRecuperacion("token-valido");
        user.setHoraRecuperacion(System.currentTimeMillis() - 60000); // 1 minuto atrás
        when(userDao.findByTokenRecuperacion("token-valido")).thenReturn(user);

        ResetPasswordRequest req = new ResetPasswordRequest("token-valido", "SuperPassword2026!", "SuperPassword2026!");
        Map<String, String> res = usersService.cambiarPasswordConToken(req);

        assertNotNull(res.get("message"));
        assertNull(user.getTokenRecuperacion(), "El token debe quedar invalidado tras su uso");
        assertNull(user.getHoraRecuperacion());
        assertNotEquals("SuperPassword2026!", user.getPassword(), "La contraseña debe estar cifrada");
        verify(userDao).save(user);
        verify(emailService).enviarCorreoPasswordModificada("carlos@ayto.es");
    }

    // -------------------------------------------------------------------------
    // RT-AUT-02 / RC-SEC-02: Validación de sesión inter-backend
    // -------------------------------------------------------------------------
    @Test
    void testValidateSession_TokenValido_DevuelveUsuarioAutenticado() {
        String token = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJjYXJsb3NAYXl0by5lcyJ9";
        when(jwtUtil.esTokenValido(anyString())).thenReturn(true);
        when(jwtUtil.getEmailDelToken(anyString())).thenReturn("carlos@ayto.es");

        User user = new User();
        user.setId("user-uuid-1");
        user.setEmail("carlos@ayto.es");
        user.setNombre("Carlos");
        user.setMunicipio("Ciudad Real");
        user.setRol("EMPLEADO_AYTO");
        user.setActivo(true);
        user.setPagadoAlta(false);
        when(userDao.findByEmail("carlos@ayto.es")).thenReturn(user);

        UsuarioValidadoDto dto = usersService.validateSession(token);

        assertNotNull(dto);
        assertEquals("user-uuid-1", dto.id());
        assertEquals("carlos@ayto.es", dto.email());
        assertEquals("Carlos", dto.nombre());
        assertEquals("Ciudad Real", dto.municipio());
        assertEquals("EMPLEADO_AYTO", dto.rol());
        assertFalse(dto.pagadoAlta());
    }

    @Test
    void testValidateSession_TokenInvalido_LanzaUnauthorized() {
        when(jwtUtil.esTokenValido("token-falso")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                usersService.validateSession("Bearer token-falso")
        );
        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    void testValidateSession_UsuarioInactivo_LanzaUnauthorized() {
        when(jwtUtil.esTokenValido(anyString())).thenReturn(true);
        when(jwtUtil.getEmailDelToken(anyString())).thenReturn("inactivo@ayto.es");

        User user = new User();
        user.setEmail("inactivo@ayto.es");
        user.setActivo(false); // Inactivo
        when(userDao.findByEmail("inactivo@ayto.es")).thenReturn(user);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                usersService.validateSession("token-valido")
        );
        assertEquals(401, ex.getStatusCode().value());
    }
}
