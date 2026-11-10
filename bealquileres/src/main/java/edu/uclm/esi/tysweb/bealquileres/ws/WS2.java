package edu.uclm.esi.tysweb.bealquileres.ws;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import edu.uclm.esi.tysweb.bealquileres.dto.VehiculoSimulador;
import edu.uclm.esi.tysweb.bealquileres.services.SimuladorClient;

@Component
public class WS2 extends TextWebSocketHandler {

    @Autowired
    private SimuladorClient simuladorClient;

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        this.sessions.put(session.getId(), session);
        System.out.println("Se ha conectado al WebSocket: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        this.sessions.remove(session.getId());
        System.out.println("Se ha desconectado del WebSocket: " + session.getId() + " (status=" + status + ")");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String texto = message.getPayload();
        try {
            JSONObject jso = new JSONObject(texto);
            String tipo = jso.optString("tipo", "");

            if ("CONSULTAR_VEHICULOS".equals(tipo)) {
                String municipio = jso.optString("municipio", "Ciudad Real");
                List<VehiculoSimulador> vehiculos = this.simuladorClient.getVehiculos(municipio);
                JSONArray jsa = new JSONArray(vehiculos);
                WebSocketMessage<?> respuesta = new TextMessage(jsa.toString());
                session.sendMessage(respuesta);
            } else if ("PING".equals(tipo)) {
                JSONObject pong = new JSONObject();
                pong.put("tipo", "PONG");
                pong.put("timestamp", System.currentTimeMillis());
                session.sendMessage(new TextMessage(pong.toString()));
            }
        } catch (Exception e) {
            System.err.println("Error procesando mensaje WebSocket: " + e.getMessage());
        }
    }

    public void broadcast(String message) {
        TextMessage textMessage = new TextMessage(message);
        for (WebSocketSession session : this.sessions.values()) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(textMessage);
                } catch (Exception ignored) {}
            }
        }
    }
}

