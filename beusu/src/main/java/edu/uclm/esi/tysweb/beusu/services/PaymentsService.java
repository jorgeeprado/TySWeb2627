package edu.uclm.esi.tysweb.beusu.services;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

import edu.uclm.esi.tysweb.beusu.dao.CompraDao;
import edu.uclm.esi.tysweb.beusu.dao.UserDao;
import edu.uclm.esi.tysweb.beusu.model.Compra;
import edu.uclm.esi.tysweb.beusu.model.User;

/**
 * Servicio de pagos con Stripe (Anexo 11.3, RN-COM-03, RN-CIU-02).
 * Gestiona el prepago (PaymentIntent) y la confirmación anti-fraude sin Webhook.
 */
@Service
public class PaymentsService {

    private static final Logger log = LoggerFactory.getLogger(PaymentsService.class);

    private final CompraDao compraDao;
    private final UserDao userDao;
    private final String stripeSecretKey;
    private final String bealquileresUrl;
    private final RestTemplate restTemplate;

    public PaymentsService(
            CompraDao compraDao,
            UserDao userDao,
            @Value("${stripe.secret.key:}") String stripeSecretKey,
            @Value("${app.bealquileres.url:http://localhost:8081}") String bealquileresUrl) {
        this.compraDao = compraDao;
        this.userDao = userDao;
        this.stripeSecretKey = stripeSecretKey;
        String rawUrl = (bealquileresUrl != null && !bealquileresUrl.isBlank()) ? bealquileresUrl : "http://localhost:8081";
        this.bealquileresUrl = rawUrl.endsWith("/") ? rawUrl.substring(0, rawUrl.length() - 1) : rawUrl;

        // Configuración de Stripe
        if (stripeSecretKey != null && !stripeSecretKey.isBlank()) {
            Stripe.apiKey = stripeSecretKey;
        }

        // RestTemplate para comunicación inter-backend con bealquileres
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);
        this.restTemplate = new RestTemplate(factory);
    }

    // -------------------------------------------------------------------------
    // PASO 2.3: prepay(sessionToken, importe) — Anexo 11.3
    // -------------------------------------------------------------------------
    public String prepay(String userEmail, Double importe, String tipo, Integer cantidad, String municipio) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email de usuario obligatorio");
        }

        if (importe == null || importe <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El importe debe ser superior a 0");
        }

        User user = this.userDao.findByEmail(userEmail.trim().toLowerCase());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }

        String tipoFinal = (tipo != null && !tipo.isBlank()) ? tipo : "BICICLETAS";
        long amountCents = Math.round(importe * 100);

        String idTransaccion;
        try {
            if (this.stripeSecretKey != null 
                    && !this.stripeSecretKey.contains("MockKey") 
                    && this.stripeSecretKey.startsWith("sk_")) {
                // Llamada oficial a Stripe SDK
                PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                        .setAmount(amountCents)
                        .setCurrency("eur")
                        .setDescription(tipoFinal + " - " + userEmail)
                        .putMetadata("userEmail", userEmail)
                        .putMetadata("tipo", tipoFinal)
                        .setAutomaticPaymentMethods(
                                PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build()
                        )
                        .build();

                PaymentIntent intent = PaymentIntent.create(params);
                idTransaccion = intent.getClientSecret();
            } else {
                // Modo desarrollo / mock offline
                idTransaccion = "pi_mock_" + UUID.randomUUID().toString().replace("-", "") 
                        + "_secret_" + UUID.randomUUID().toString().replace("-", "");
            }
        } catch (Exception e) {
            log.warn("Fallo al conectar con Stripe API ({}). Usando transacción simulada.", e.getMessage());
            idTransaccion = "pi_mock_" + UUID.randomUUID().toString().replace("-", "") 
                    + "_secret_" + UUID.randomUUID().toString().replace("-", "");
        }

        // Crear y persistir la entidad Compra en estado pendiente
        Compra compra = new Compra(user, importe, tipoFinal, cantidad, municipio);
        compra.setIdTransaccion(idTransaccion);
        this.compraDao.save(compra);

        return idTransaccion;
    }

    // -------------------------------------------------------------------------
    // PASO 4.3: confirm(idTransaccion) — Anexo 11.3 (con verificación anti-fraude)
    // -------------------------------------------------------------------------
    public Map<String, String> confirm(String idTransaccion) {
        if (idTransaccion == null || idTransaccion.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "idTransaccion es obligatorio");
        }

        Optional<Compra> compraOpt = this.compraDao.findByIdTransaccion(idTransaccion.trim());
        if (compraOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transacción no encontrada");
        }

        Compra compra = compraOpt.get();

        if (compra.isPagado()) {
            return Map.of("message", "Pago realizado correctamente (ya confirmado previamente)");
        }

        // CONTROL ANTI-FRAUDE SIN WEBHOOK (Anexo 11.3):
        // Verificamos directamente contra la API de Stripe que el PaymentIntent esté en estado "succeeded"
        if (!idTransaccion.startsWith("pi_mock_")) {
            try {
                String intentId = idTransaccion.contains("_secret_") 
                        ? idTransaccion.substring(0, idTransaccion.indexOf("_secret_")) 
                        : idTransaccion;

                PaymentIntent intent = PaymentIntent.retrieve(intentId);
                String status = intent.getStatus();

                if (!"succeeded".equalsIgnoreCase(status) && !"processing".equalsIgnoreCase(status)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Fraude detectado: El pago no ha sido completado en la pasarela Stripe (estado: " + status + ")");
                }
            } catch (ResponseStatusException rse) {
                throw rse;
            } catch (Exception e) {
                log.error("Error al consultar el estado del PaymentIntent en Stripe: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo verificar el pago en Stripe: " + e.getMessage());
            }
        }

        // Transacción verificada con éxito: marcar compra como pagada
        compra.setPagado(true);
        this.compraDao.save(compra);

        // Si es alta de ciudadano (RN-CIU-02), habilitar derecho de alquiler
        if ("ALTA_CIUDADANO".equalsIgnoreCase(compra.getTipo())) {
            User user = compra.getUser();
            user.setPagadoAlta(true);
            this.userDao.save(user);
            log.info("Pago de alta de 100€ confirmado para ciudadano: {}", user.getEmail());
        }

        // Si es compra de bicicletas (RN-COM-03), consolidar definitivamente en bealquileres
        if ("BICICLETAS".equalsIgnoreCase(compra.getTipo()) && compra.getMunicipio() != null) {
            notificarConfirmacionABealquileres(compra.getMunicipio(), compra.getCantidad());
        }

        return Map.of("message", "Pago realizado correctamente");
    }

    private void notificarConfirmacionABealquileres(String municipio, Integer cantidad) {
        try {
            String url = this.bealquileresUrl + "/api/vehiculos/confirmarCompra";
            Map<String, Object> body = Map.of(
                    "municipio", municipio,
                    "cantidad", cantidad != null ? cantidad : 0
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            this.restTemplate.postForEntity(url, entity, Map.class);
            log.info("Asignación definitiva consolidada en bealquileres para el municipio {}", municipio);
        } catch (Exception e) {
            log.warn("No se pudo notificar la confirmación definitiva a bealquileres ({}): {}", this.bealquileresUrl, e.getMessage());
        }
    }
}
