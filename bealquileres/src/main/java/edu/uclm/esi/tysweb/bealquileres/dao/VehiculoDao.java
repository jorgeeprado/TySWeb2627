package edu.uclm.esi.tysweb.bealquileres.dao;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import edu.uclm.esi.tysweb.bealquileres.model.Vehiculo;

public interface VehiculoDao extends JpaRepository<Vehiculo, Long> {
    List<Vehiculo> findByMunicipioIsNull();
    List<Vehiculo> findByMunicipioName(String name);
    List<Vehiculo> findByEstacionId(Long estacionId);
    int countByEstacionId(Long estacionId);
    List<Vehiculo> findByMunicipioNameAndEstacionIsNull(String name);
}
