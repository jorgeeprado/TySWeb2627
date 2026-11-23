package edu.uclm.esi.tysweb.bealquileres.dao;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.uclm.esi.tysweb.bealquileres.model.Vehiculo;

public interface VehiculoDao extends JpaRepository<Vehiculo, Long> {
    List<Vehiculo> findByMunicipioIsNull();
    List<Vehiculo> findByMunicipioName(String name);
    List<Vehiculo> findByEstacionId(Long estacionId);
    List<Vehiculo> findByEstacionIdAndActivoTrue(Long estacionId);
    int countByEstacionId(Long estacionId);
    List<Vehiculo> findByMunicipioNameAndEstacionIsNull(String name);

    // Consulta para obtener bicicletas disponibles para prerreserva:
    // 1) Sin municipio asignado (RN-COM-01)
    // 2) O cuya prerreserva anterior haya caducado (más de 30 minutos, RN-COM-02)
    @Query("SELECT v FROM Vehiculo v WHERE v.municipio IS NULL OR (v.horaDePrerreserva IS NOT NULL AND v.horaDePrerreserva < :expiracion)")
    List<Vehiculo> findDisponiblesParaPrerreserva(@Param("expiracion") LocalDateTime expiracion);

    // Consulta para obtener las bicicletas prerreservadas pendientes de confirmación definitiva de pago
    List<Vehiculo> findByMunicipioNameAndHoraDePrerreservaIsNotNull(String name);

    List<Vehiculo> findByMunicipioIdAndHoraDePrerreservaIsNotNull(Long municipioId);
}
