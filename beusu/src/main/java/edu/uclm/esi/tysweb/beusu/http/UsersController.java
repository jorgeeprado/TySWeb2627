package edu.uclm.esi.tysweb.beusu.http;

import java.net.URI;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import edu.uclm.esi.tysweb.beusu.dto.LoginRequest;
import edu.uclm.esi.tysweb.beusu.dto.LoginResponse;
import edu.uclm.esi.tysweb.beusu.dto.RegistroRequest;
import edu.uclm.esi.tysweb.beusu.dto.ResetPasswordRequest;
import edu.uclm.esi.tysweb.beusu.dto.SolicitarRecuperacionRequest;
import edu.uclm.esi.tysweb.beusu.dto.UsuarioValidadoDto;
import edu.uclm.esi.tysweb.beusu.services.UsersService;
import jakarta.validation.Valid;

/**
 * Diagrama de clases: <<RestController>> UsersController
 *   - service: UsersService
 *   + registerMunicipio(body)
 *   + activate2FA(email)
 *   + verify2FACode(body)
 *   + solicitarRecuperacion(body)
 *   + cambiarPassword(body)
 *   + validateSession(token)
 */
@RestController
@RequestMapping("/users")
@CrossOrigin("*")
public class UsersController {

    @Autowired
    private UsersService service;

    // -------------------------------------------------------------------------
    // GET /users/health — Comprobación de estado para frontend / monitorización
    // -------------------------------------------------------------------------
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "beusu", "timestamp", System.currentTimeMillis()));
    }

    @GetMapping("/login")
    public ResponseEntity<Map<String, Object>> loginCheck() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "beusu"));
    }

    // -------------------------------------------------------------------------
    // PUT / POST /users/registerMunicipio  — Registro de empleado municipal
    // -------------------------------------------------------------------------
    @PutMapping("/registerMunicipio")
    public ResponseEntity<Map<String, Object>> registerMunicipioPut(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.ok(this.service.registerMunicipio(request));
    }

    @PostMapping("/registerMunicipio")
    public ResponseEntity<Map<String, Object>> registerMunicipioPost(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.ok(this.service.registerMunicipio(request));
    }

    // -------------------------------------------------------------------------
    // POST /users/activarDirecto — Activación inmediata en desarrollo/docencia
    // -------------------------------------------------------------------------
    @PostMapping("/activarDirecto")
    public ResponseEntity<Map<String, Object>> activarDirecto(@RequestBody(required = false) Map<String, String> body) {
        String email = body != null ? body.get("email") : null;
        String token = body != null ? body.get("token") : null;
        edu.uclm.esi.tysweb.beusu.model.User user = this.service.activarDirecto(email, token);
        return ResponseEntity.ok(Map.of(
            "message", "Cuenta activada correctamente",
            "email", user.getEmail(),
            "activo", user.getActivo()
        ));
    }

    // -------------------------------------------------------------------------
    // GET /users/confirmar?token=...  — Enlace de confirmación de cuenta
    // -------------------------------------------------------------------------
    @GetMapping("/confirmar")
    public ResponseEntity<Void> confirmarCuenta(@RequestParam("token") String token) {
        this.service.confirmarCuenta(token);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("http://localhost:4200/?confirmado=true"))
                .build();
    }

    // -------------------------------------------------------------------------
    // POST /users/login  — Inicio de sesión
    // -------------------------------------------------------------------------
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return this.service.login(request);
    }

    @PutMapping("/login")
    public LoginResponse loginPut(@Valid @RequestBody LoginRequest request) {
        return this.service.login(request);
    }

    // -------------------------------------------------------------------------
    // POST /users/activate2FA  — 2FA Fase 1: Generar QR y secreto
    // -------------------------------------------------------------------------
    @PostMapping("/activate2FA")
    public ResponseEntity<Map<String, String>> activate2FA(
            @RequestParam(value = "email", required = false) String emailParam,
            @RequestBody(required = false) Map<String, String> body) {
        String email = emailParam != null ? emailParam : (body != null ? body.get("email") : null);
        String qrUrl = this.service.activar2FA(email);
        return ResponseEntity.ok(Map.of("qrUrl", qrUrl));
    }

    // -------------------------------------------------------------------------
    // POST /users/verify2FACode  — 2FA Fase 2: Verificar código TOTP
    // -------------------------------------------------------------------------
    @PostMapping("/verify2FACode")
    public LoginResponse verify2FACode(@RequestBody Map<String, Object> body) {
        return this.service.confirm2faCode(body);
    }

    // -------------------------------------------------------------------------
    // POST /users/solicitarRecuperacion  — Recuperación de contraseña (RN-CUE-04, RC-SEC-03)
    // -------------------------------------------------------------------------
    @PostMapping("/solicitarRecuperacion")
    public ResponseEntity<Map<String, String>> solicitarRecuperacion(@Valid @RequestBody SolicitarRecuperacionRequest request) {
        return ResponseEntity.ok(this.service.solicitarRecuperacionPwd(request.email()));
    }

    // -------------------------------------------------------------------------
    // POST /users/cambiarPassword  — Cambio con token (RN-CUE-04, RT-CONF-02)
    // -------------------------------------------------------------------------
    @PostMapping("/cambiarPassword")
    public ResponseEntity<Map<String, String>> cambiarPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(this.service.cambiarPasswordConToken(request));
    }

    // -------------------------------------------------------------------------
    // GET / POST /users/validateSession  — Validación inter-backend (RT-AUT-02, RC-SEC-02)
    // -------------------------------------------------------------------------
    @GetMapping("/validateSession")
    public ResponseEntity<UsuarioValidadoDto> validateSession(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String tokenParam) {
        String token = (authHeader != null && !authHeader.isBlank()) ? authHeader : tokenParam;
        return ResponseEntity.ok(this.service.validateSession(token));
    }

    @PostMapping("/validateSession")
    public ResponseEntity<UsuarioValidadoDto> validateSessionPost(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) Map<String, String> body) {
        String token = (authHeader != null && !authHeader.isBlank()) 
                ? authHeader 
                : (body != null ? body.get("token") : null);
        return ResponseEntity.ok(this.service.validateSession(token));
    }
}
