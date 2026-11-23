package edu.uclm.esi.tysweb.bealquileres.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import edu.uclm.esi.tysweb.bealquileres.model.Alquiler;

public interface AlquilerDao extends JpaRepository<Alquiler, Long> {

    // RN-ALQ-01(c): Comprobar si el ciudadano tiene ya un alquiler activo pendiente de devolución
    Optional<Alquiler> findByUsuarioEmailAndActivoTrue(String usuarioEmail);

    // Buscar alquiler activo por ID de vehículo
    Optional<Alquiler> findByVehiculoIdAndActivoTrue(Long vehiculoId);

    List<Alquiler> findByUsuarioEmailOrderByFechaInicioDesc(String usuarioEmail);
}
