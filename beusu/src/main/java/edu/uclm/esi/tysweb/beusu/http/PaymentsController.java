package edu.uclm.esi.tysweb.beusu.http;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.esi.tysweb.beusu.JwtUtil;
import edu.uclm.esi.tysweb.beusu.dto.ConfirmPaymentRequest;
import edu.uclm.esi.tysweb.beusu.dto.PrepayRequest;
import edu.uclm.esi.tysweb.beusu.services.PaymentsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Controlador REST para el flujo de pagos con Stripe (Anexo 11.3 y RN-CIU-02).
 * Mensajes:
 *   - 2.3 prepay(sessionToken, importe) -> devuelve idTransaccion
 *   - 4.3 confirm(idTransaccion) -> valida pago en Stripe y consolida
 */
@RestController
@RequestMapping("/payments")
@CrossOrigin("*")
public class PaymentsController {

    @Autowired
    private PaymentsService paymentsService;

    @Autowired
    private JwtUtil jwtUtil;

    // -------------------------------------------------------------------------
    // PASO 2.3: prepay (compra de bicicletas o pagos generales)
    // -------------------------------------------------------------------------
    @PostMapping("/prepay")
    public ResponseEntity<Map<String, String>> prepay(
            @RequestBody(required = false) PrepayRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "email", required = false) String emailParam,
            HttpServletRequest httpRequest) {

        String userEmail = resolveEmail(authHeader, emailParam, httpRequest);

        Double importe = request != null ? request.importe() : null;
        String tipo = request != null ? request.tipo() : "BICICLETAS";
        Integer cantidad = request != null ? request.cantidad() : null;
        String municipio = request != null ? request.municipio() : null;

        if (importe == null && cantidad != null) {
            importe = (double) (cantidad * 1200); // Precio de referencia según RN-COM-04
        }

        if (importe == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se requiere especificar el importe o la cantidad");
        }

        String idTransaccion = this.paymentsService.prepay(userEmail, importe, tipo, cantidad, municipio);
        return ResponseEntity.ok(Map.of("idTransaccion", idTransaccion));
    }

    // -------------------------------------------------------------------------
    // PASO 2.3 (Variante): Pago único de 100€ de alta de ciudadano (RN-CIU-02)
    // -------------------------------------------------------------------------
    @PostMapping("/prepayAltaCiudadano")
    public ResponseEntity<Map<String, String>> prepayAltaCiudadano(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "email", required = false) String emailParam,
            HttpServletRequest httpRequest) {

        String userEmail = resolveEmail(authHeader, emailParam, httpRequest);
        // RN-CIU-02: Pago único de 100 euros
        Double importe = 100.0;
        String idTransaccion = this.paymentsService.prepay(userEmail, importe, "ALTA_CIUDADANO", 1, null);
        return ResponseEntity.ok(Map.of("idTransaccion", idTransaccion));
    }

    // -------------------------------------------------------------------------
    // PASO 4.3: confirm (validación anti-fraude y consolidación)
    // -------------------------------------------------------------------------
    @PostMapping("/confirm")
    public ResponseEntity<Map<String, String>> confirm(
            @Valid @RequestBody(required = false) ConfirmPaymentRequest request,
            @RequestParam(value = "idTransaccion", required = false) String idTransaccionParam,
            @RequestBody(required = false) Map<String, String> bodyMap) {

        String idTransaccion = null;
        if (request != null && request.idTransaccion() != null && !request.idTransaccion().isBlank()) {
            idTransaccion = request.idTransaccion();
        } else if (idTransaccionParam != null && !idTransaccionParam.isBlank()) {
            idTransaccion = idTransaccionParam;
        } else if (bodyMap != null && bodyMap.containsKey("idTransaccion")) {
            idTransaccion = bodyMap.get("idTransaccion");
        }

        if (idTransaccion == null || idTransaccion.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "idTransaccion ausente");
        }

        Map<String, String> result = this.paymentsService.confirm(idTransaccion);
        return ResponseEntity.ok(result);
    }

    private String resolveEmail(String authHeader, String emailParam, HttpServletRequest request) {
        if (emailParam != null && !emailParam.isBlank()) {
            return emailParam.trim().toLowerCase();
        }

        if (authHeader != null && !authHeader.isBlank()) {
            String token = authHeader.trim();
            if (token.startsWith("Bearer ") || token.startsWith("bearer ")) {
                token = token.substring(7).trim();
            }
            String email = this.jwtUtil.getEmailDelToken(token);
            if (email != null && !email.isBlank()) {
                return email.trim().toLowerCase();
            }
        }

        Object userAttr = request.getAttribute("authenticatedUser");
        if (userAttr != null) {
            return userAttr.toString().trim().toLowerCase();
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado para realizar el pago");
    }
}
