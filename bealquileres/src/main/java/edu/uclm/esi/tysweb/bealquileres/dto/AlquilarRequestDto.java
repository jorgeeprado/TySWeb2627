package edu.uclm.esi.tysweb.bealquileres.dto;

public class AlquilarRequestDto {
    private Long vehiculoId;
    private Long estacionDestinoId;

    public AlquilarRequestDto() {}

    public AlquilarRequestDto(Long vehiculoId, Long estacionDestinoId) {
        this.vehiculoId = vehiculoId;
        this.estacionDestinoId = estacionDestinoId;
    }

    public Long getVehiculoId() {
        return vehiculoId;
    }

    public void setVehiculoId(Long vehiculoId) {
        this.vehiculoId = vehiculoId;
    }

    public Long getEstacionDestinoId() {
        return estacionDestinoId;
    }

    public void setEstacionDestinoId(Long estacionDestinoId) {
        this.estacionDestinoId = estacionDestinoId;
    }
}
