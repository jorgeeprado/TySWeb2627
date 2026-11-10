package edu.uclm.esi.tysweb.bealquileres.http;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.esi.tysweb.bealquileres.dto.AsignarVehiculoDto;
import edu.uclm.esi.tysweb.bealquileres.dto.VehiculoDto;
import edu.uclm.esi.tysweb.bealquileres.services.VehiculoService;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/vehiculos")
@CrossOrigin("*")
@Tag(name = "Vehículos", description = "Operaciones relacionadas con la gestión de vehículos")
public class VehiculoController {

    @Autowired 
    private VehiculoService service; // El controlador inyecta el servicio

    @GetMapping("/health")
    public java.util.Map<String, Object> health() {
        return java.util.Map.of("status", "UP", "service", "bealquileres");
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

    // Método alineado con el código de clase
    @PostMapping("/asignarVehiculos")
    public void asignarVehiculos(@RequestBody AsignarVehiculoDto request) {
        
        // La validación ahora es directa porque Spring ya nos da los tipos correctos
        if (request.municipio() == null || request.cantidad() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tienes que indicar municipio y cantidad");
        }
        
        if (request.cantidad() < 1 || request.cantidad() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad debe estar entre 1 y 100");
        }
        
        // Pasamos los datos limpios al servicio
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