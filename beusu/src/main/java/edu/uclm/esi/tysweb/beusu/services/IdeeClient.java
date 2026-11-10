package edu.uclm.esi.tysweb.beusu.services;

import java.time.Duration;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;

/**
 * <<Service>> IdeeClient
 * - restClient: RestClient
 * + existe(municipio): boolean
 * Consulta la API oficial española idee.es para validar si un municipio existe.
 */
@Service
public class IdeeClient {

    private final RestClient restClient;

    public IdeeClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(1200));
        factory.setReadTimeout(Duration.ofMillis(1500));

        this.restClient = RestClient.builder()
                .baseUrl("https://api-features.idee.es")
                .requestFactory(factory)
                .build();
    }

    // existe(municipio): boolean (diagrama de la diapositiva)
    public boolean existe(String municipio) {
        return existsMunicipio(municipio);
    }

    public boolean existsMunicipio(String municipio) {
        if (municipio == null || municipio.trim().isEmpty()) {
            return false;
        }
        try {
            String response = this.restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/collections/address/items")
                            .queryParam("component_AddressAreaName", municipio.trim())
                            .queryParam("f", "json")
                            .queryParam("limit", 1)
                            .build())
                    .retrieve()
                    .body(String.class);

            if (response == null || response.trim().isEmpty()) {
                return false;
            }

            // Si contiene "numberMatched": 0 o similar
            if (response.contains("\"numberMatched\":0") || response.contains("\"numberMatched\": 0")) {
                return false;
            }

            return response.contains("numberMatched") || response.contains("features");
        } catch (Exception e) {
            // Tolerancia a fallos: si idee.es está caída o sin respuesta (timeout),
            // no bloqueamos el registro si el nombre del municipio es válido
            System.err.println("[IdeeClient] Aviso: consulta a idee.es no disponible (" + e.getMessage() + "). Aplicando tolerancia a fallos.");
            return municipio.trim().length() >= 2;
        }
    }
}
