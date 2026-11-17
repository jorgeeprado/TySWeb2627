package edu.uclm.esi.tysweb.bealquileres.http;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.esi.tysweb.bealquileres.dto.AsignarVehiculoDto;
import edu.uclm.esi.tysweb.bealquileres.dto.ConfirmarCompraDto;
import edu.uclm.esi.tysweb.bealquileres.dto.PrerreservaDto;
import edu.uclm.esi.tysweb.bealquileres.dto.UsuarioAutenticadoDto;
import edu.uclm.esi.tysweb.bealquileres.dto.VehiculoDto;
import edu.uclm.esi.tysweb.bealquileres.services.VehiculoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/vehiculos")
@CrossOrigin("*")
@Tag(name = "Vehículos", description = "Operaciones relacionadas con la compra, prerreserva y gestión de vehículos")
public class VehiculoController {

    @Autowired 
    private VehiculoService service;

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "service", "bealquileres");
    }

    // -------------------------------------------------------------------------
    // RN-COM-04: Precio unitario de la bicicleta (1200 euros)
    // -------------------------------------------------------------------------
    @GetMapping("/getPrecioDeBicicleta")
    @Operation(summary = "Devuelve el precio unitario de una bicicleta (RN-COM-04: 1200 euros)")
    public ResponseEntity<Integer> getPrecioDeBicicleta() {
        return ResponseEntity.ok(this.service.getPrecioDeBicicleta());
    }

    // -------------------------------------------------------------------------
    // PRERRESERVA DE BICICLETAS (RN-COM-01, RN-COM-02, RC-CON-03, Anexo 11.3)
    // -------------------------------------------------------------------------
    @PostMapping("/prerreservar")
    @Operation(summary = "Preasigna temporalmente un conjunto de bicicletas al municipio durante 30 minutos")
    public ResponseEntity<List<VehiculoDto>> prerreservar(
            @RequestBody(required = false) PrerreservaDto request,
            @RequestParam(required = false) Integer cantidadParam,
            @RequestParam(required = false) String municipioParam,
            HttpServletRequest httpRequest) {

        Integer cantidad = (request != null && request.cantidad() != null) ? request.cantidad() : cantidadParam;
        String municipio = (request != null && request.municipio() != null && !request.municipio().isBlank()) 
                ? request.municipio() 
                : municipioParam;

        // Si el municipio no vino en el body/query, extraerlo del usuario autenticado (RT-AUT-02)
        if (municipio == null || municipio.isBlank()) {
            UsuarioAutenticadoDto usuario = (UsuarioAutenticadoDto) httpRequest.getAttribute("usuarioAutenticado");
            if (usuario != null && usuario.municipio() != null) {
                municipio = usuario.municipio();
            }
        }

        if (cantidad == null || cantidad < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se requiere especificar una cantidad válida");
        }

        if (municipio == null || municipio.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Municipio no determinado para la prerreserva");
        }

        List<VehiculoDto> prerreservadas = this.service.prerreservar(municipio, cantidad);
        return ResponseEntity.ok(prerreservadas);
    }

    // -------------------------------------------------------------------------
    // CONFIRMAR ASIGNACIÓN DEFINITIVA TRAS EL PAGO (RN-COM-03, Anexo 11.3)
    // -------------------------------------------------------------------------
    @PostMapping("/confirmarCompra")
    @Operation(summary = "Consolida definitivamente las bicicletas preasignadas a un municipio tras confirmar el pago")
    public ResponseEntity<Map<String, Object>> confirmarCompra(
            @RequestBody ConfirmarCompraDto request,
            HttpServletRequest httpRequest) {

        String municipio = request != null ? request.municipio() : null;
        Integer cantidad = request != null ? request.cantidad() : null;

        if (municipio == null || municipio.isBlank()) {
            UsuarioAutenticadoDto usuario = (UsuarioAutenticadoDto) httpRequest.getAttribute("usuarioAutenticado");
            if (usuario != null) {
                municipio = usuario.municipio();
            }
        }

        if (municipio == null || municipio.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Municipio no especificado");
        }

        this.service.confirmarCompra(municipio, cantidad);
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "message", "Compra confirmada y bicicletas asignadas definitivamente al municipio " + municipio
        ));
    }

    @PostMapping("/createVehicles")
    public void createVehicles(@RequestBody Integer numberOfVehicles) {
        if (numberOfVehicles > 5) 
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "Demasiados vehículos. Se admiten hasta 5");
        this.service.createVehicles(numberOfVehicles);
    }

    @GetMapping("/getVehiculos")
    public List<VehiculoDto> getVehiculos() {
        return this.service.getVehiculos();
    }

    @PostMapping("/asignarVehiculos")
    public void asignarVehiculos(@RequestBody AsignarVehiculoDto request) {
        if (request.municipio() == null || request.cantidad() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tienes que indicar municipio y cantidad");
        }
        
        if (request.cantidad() < 1 || request.cantidad() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad debe estar entre 1 y 100");
        }
        
        this.service.asignarVehiculos(request.municipio(), request.cantidad());
    }

    @PostMapping("/ubicarEnEstacion")
    public void ubicarEnEstacion(@RequestBody edu.uclm.esi.tysweb.bealquileres.dto.UbicarEnEstacionDto request) {
        if (request.estacionId() == null || request.cantidad() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se requiere el ID de la estación y la cantidad");
        }
        this.service.ubicarEnEstacion(request.estacionId(), request.cantidad());
    }
}
