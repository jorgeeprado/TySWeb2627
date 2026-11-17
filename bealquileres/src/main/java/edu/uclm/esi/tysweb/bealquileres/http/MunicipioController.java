package edu.uclm.esi.tysweb.bealquileres.http;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.esi.tysweb.bealquileres.dto.FeatureSimpleDto;
import edu.uclm.esi.tysweb.bealquileres.dto.MunicipioRequest;
import edu.uclm.esi.tysweb.bealquileres.dto.SetEstacionDtoRequest;
import edu.uclm.esi.tysweb.bealquileres.dto.UsuarioAutenticadoDto;
import edu.uclm.esi.tysweb.bealquileres.model.Municipio;
import edu.uclm.esi.tysweb.bealquileres.services.MunicipioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/municipios")
@CrossOrigin("*")
@Tag(
    name = "Municipios",
    description = "Operaciones relacionadas con la gestión de municipios por parte de los ayuntamientos"
)
public class MunicipioController {

    private final MunicipioService municipioService;

    public MunicipioController(MunicipioService municipioService) {
        this.municipioService = municipioService;
    }

    @GetMapping("/getNumeroDeDirecciones") @Operation(summary = "Devuelve el número de direcciones del municipio pasado como parámetro")
    public Integer getNumeroDeCalles(@RequestParam @Parameter(description = "Nombre de la ciudad") String municipio) {
        return this.municipioService.getNumeroDeDirecciones(municipio);
    }

    @PostMapping("/load")
    @Operation(summary = "Carga en la base de datos las direcciones del municipio")
    public void load(@RequestParam @Parameter(description = "Índice de la dirección inicial (0, por ejemplo)") Integer offset, 
        @RequestParam  @Parameter(description = "Índice de la dirección final") Integer limit, 
        @RequestParam(required = false) @Parameter(description = "Mostrar o no progreso") Boolean progress, 
        @RequestBody MunicipioRequest request) {
            
        municipioService.load(request.municipio(), offset, limit, progress);


    }


    @PostMapping("/especificar")
    @Operation(summary = "Especifica o registra el municipio en bealquileres (RN-MUN-01)")
    public java.util.Map<String, Object> especificar(
            @RequestBody(required = false) MunicipioRequest request,
            @RequestParam(required = false) String municipio,
            HttpServletRequest httpRequest) {

        String nombreMuni = (request != null && request.municipio() != null && !request.municipio().isBlank())
                ? request.municipio()
                : municipio;

        if (nombreMuni == null || nombreMuni.isBlank()) {
            UsuarioAutenticadoDto usuario = (UsuarioAutenticadoDto) httpRequest.getAttribute("usuarioAutenticado");
            if (usuario != null) {
                nombreMuni = usuario.municipio();
            }
        }

        if (nombreMuni == null || nombreMuni.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Municipio no especificado");
        }

        Municipio m = this.municipioService.especificarMunicipio(nombreMuni);
        return java.util.Map.of(
                "id", m.getId(),
                "nombre", m.getName(),
                "status", "OK"
        );
    }

    @GetMapping("/getFeatures")
    @Operation(summary = "Devuelve los puntos de interés o calles (Feature) cargados para el municipio")
    public java.util.List<FeatureSimpleDto> getFeatures(@RequestParam String municipio) {
        return this.municipioService.getFeatures(municipio);
    }

    @GetMapping("/getEstaciones")
    @Operation(summary = "Devuelve las estaciones creadas en el municipio")
    public java.util.List<edu.uclm.esi.tysweb.bealquileres.dto.EstacionDto> getEstaciones(@RequestParam String municipio) {
        return this.municipioService.getEstaciones(municipio);
    }

    @PostMapping("/setEstacion")
    @Operation(summary = "Crea una nueva estación de recarga a partir de una dirección existente o de coordenadas del mapa")
    public void setEstacion(@RequestBody SetEstacionDtoRequest request) {
        
        if (request.municipio() == null || request.name() == null || request.number() == null || request.capacidad() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faltan datos obligatorios, incluyendo la capacidad");
        }

        this.municipioService.setEstacion(
            request.municipio(), 
            request.name(), 
            request.number(), 
            request.capacidad(),
            request.latitude(),
            request.longitude(),
            request.zipCode()
        );
    }
}


