package edu.uclm.esi.tysweb.bealquileres.services;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import edu.uclm.esi.tysweb.bealquileres.dto.UsuarioAutenticadoDto;

/**
 * Cliente HTTP para la validación centralizada de sesiones contra beusu.
 * Cumple con RT-AUT-02 y RC-SEC-02.
 */
@Service
public class BeusuAuthClient {

    private static final Logger log = LoggerFactory.getLogger(BeusuAuthClient.class);

    private final RestTemplate restTemplate;
    private final String beusuUrl;

    public BeusuAuthClient(@Value("${app.beusu.url:http://localhost:8080}") String beusuUrl) {
        this.beusuUrl = beusuUrl.endsWith("/") ? beusuUrl.substring(0, beusuUrl.length() - 1) : beusuUrl;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Valida el token contra beusu (GET /users/validateSession) y devuelve los datos del usuario.
     * @param token Token JWT recibido en la cabecera Authorization o parámetro.
     * @return Optional con el UsuarioAutenticadoDto si es válido, Optional.empty() en caso contrario.
     */
    public Optional<UsuarioAutenticadoDto> validarToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        String bearerToken = token.trim();
        if (!bearerToken.startsWith("Bearer ") && !bearerToken.startsWith("bearer ")) {
            bearerToken = "Bearer " + bearerToken;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", bearerToken);
            headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = this.beusuUrl + "/users/validateSession";
            ResponseEntity<UsuarioAutenticadoDto> response = this.restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    UsuarioAutenticadoDto.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.of(response.getBody());
            }
        } catch (HttpStatusCodeException e) {
            log.warn("Validación de sesión rechazada por beusu (HTTP {}): {}", e.getStatusCode(), e.getMessage());
        } catch (RestClientException e) {
            log.error("Error al conectar con beusu en {}: {}", this.beusuUrl, e.getMessage());
        } catch (Exception e) {
            log.error("Excepción inesperada al validar sesión en beusu: {}", e.getMessage());
        }
        return Optional.empty();
    }
}
