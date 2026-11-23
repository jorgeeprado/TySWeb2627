package edu.uclm.esi.tysweb.simulador.model;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

import edu.uclm.esi.tysweb.simulador.dto.Instruction;

public class VehiculoThread implements Runnable {

    private static final int INTERVALO_SEGUNDOS = 4;

    private final Vehiculo vehiculo;

    public VehiculoThread(Vehiculo vehiculo) {
        this.vehiculo = vehiculo;
    }

    private double metrosPorTick() {
        return this.vehiculo.getMps() * INTERVALO_SEGUNDOS;
    }

    @Override
    public void run() {
        List<Instruction> instrucciones = vehiculo.getInstrucciones();

        if (instrucciones == null || instrucciones.isEmpty()) {
            if (vehiculo.getVehiculoId() != null) {
                notificarDevolucionBealquileres();
            }
            return;
        }

        int instructionIndex = 0;
        double recorridaEnInstruccion = 0.0;

        Instruction primera = instrucciones.get(0);

        vehiculo.setCurrentLongitude(primera.orig().get(0));
        vehiculo.setCurrentLatitude(primera.orig().get(1));

        while (instructionIndex < instrucciones.size()) {
            try {
                Thread.sleep(INTERVALO_SEGUNDOS * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            double distanciaDisponible = this.metrosPorTick();
            this.vehiculo.recalculateKmh();
            Instruction instruccionActual = null;

            while (distanciaDisponible > 0 && instructionIndex < instrucciones.size()) {
                Instruction instruccion = instrucciones.get(instructionIndex);
                instruccionActual = instruccion;

                double distanciaInstruccion = parseDistance(instruccion.distance());
                double distanciaRestante = distanciaInstruccion - recorridaEnInstruccion;
                double avance = Math.min(distanciaDisponible, distanciaRestante);

                recorridaEnInstruccion += avance;
                distanciaDisponible -= avance;

                vehiculo.addDistanciaRecorrida(avance);

                // RN-BIC-01, RN-BIC-02: Decrementar batería a razón de 50 km de autonomía por el 100%
                // 100% / 50 km = 2.0% por kilómetro = 0.002% por metro
                double consumoBateria = (avance / 1000.0) * 2.0;
                vehiculo.restarBateria(consumoBateria);

                double porcentaje = (distanciaInstruccion > 0) ? (recorridaEnInstruccion / distanciaInstruccion) : 1.0;

                actualizarPosicion(instruccion, Math.min(1.0, porcentaje));

                if (recorridaEnInstruccion >= distanciaInstruccion) {
                    vehiculo.setCurrentLongitude(instruccion.dest().get(0));
                    vehiculo.setCurrentLatitude(instruccion.dest().get(1));

                    instructionIndex++;
                    recorridaEnInstruccion = 0.0;
                }
            }

            if (instruccionActual != null) {
                vehiculo.setCurrentStreet(instruccionActual.description());
            }
        }

        // RN-ALQ-03: Al llegar a las coordenadas de destino, invoca automáticamente por HTTP el endpoint Dejar en bealquileres
        if (vehiculo.getVehiculoId() != null) {
            notificarDevolucionBealquileres();
        }
    }

    private void notificarDevolucionBealquileres() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();

            String matriculaStr = String.format("%04d", vehiculo.getMatricula());
            String beUrl = vehiculo.getBealquileresUrl() != null ? vehiculo.getBealquileresUrl() : "http://localhost:8081";

            String json = String.format(Locale.US,
                "{\"vehiculoId\":%d,\"matricula\":\"%s\",\"latitud\":%.6f,\"longitud\":%.6f,\"distanciaRecorrida\":%.2f,\"bateria\":%.2f,\"estacionDestinoId\":%s}",
                vehiculo.getVehiculoId(),
                matriculaStr,
                vehiculo.getCurrentLatitude() != null ? vehiculo.getCurrentLatitude() : 38.9861,
                vehiculo.getCurrentLongitude() != null ? vehiculo.getCurrentLongitude() : -3.9273,
                vehiculo.getDistanciaRecorrida() / 1000.0,
                vehiculo.getBateria(),
                vehiculo.getEstacionDestinoId() != null ? vehiculo.getEstacionDestinoId().toString() : "null"
            );

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(beUrl + "/api/alquileres/dejar"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            System.out.println("RN-ALQ-03: Devolución automática de bicicleta " + vehiculo.getVehiculoId() + " a bealquileres -> Status " + resp.statusCode());
        } catch (Exception e) {
            System.err.println("Aviso: No se pudo notificar devolución automática a bealquileres (" + e.getMessage() + ")");
        }
    }

    private void actualizarPosicion(Instruction instruccion, double porcentaje) {
        double origLon = instruccion.orig().get(0);
        double origLat = instruccion.orig().get(1);

        double destLon = instruccion.dest().get(0);
        double destLat = instruccion.dest().get(1);

        vehiculo.setCurrentLongitude(origLon + (destLon - origLon) * porcentaje);
        vehiculo.setCurrentLatitude(origLat + (destLat - origLat) * porcentaje);
    }

    private void mostrarPosicion() {
        System.out.printf(
                "Posición: %.6f, %.6f - %s - Distancia recorrida: %.2f m - Batería: %.1f%%%n",
                vehiculo.getCurrentLatitude(),
                vehiculo.getCurrentLongitude(),
                vehiculo.getCurrentStreet(),
                vehiculo.getDistanciaRecorrida(),
                vehiculo.getBateria());
    }

    private double parseDistance(String distance) {
        if (distance == null) return 100.0;
        String value = distance
                .trim()
                .toLowerCase()
                .replace(",", ".");

        if (value.endsWith("km"))
            return Double.parseDouble(value.replace("km", "").trim()) * 1000.0;

        return Double.parseDouble(value.replace("m", "").trim());
    }
}
