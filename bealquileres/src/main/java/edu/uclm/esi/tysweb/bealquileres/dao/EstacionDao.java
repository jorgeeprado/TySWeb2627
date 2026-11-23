package edu.uclm.esi.tysweb.bealquileres.dao;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import edu.uclm.esi.tysweb.bealquileres.model.Estacion;

public interface EstacionDao extends JpaRepository<Estacion, Long> {
    List<Estacion> findByMunicipioName(String name);
    List<Estacion> findByMunicipioId(String municipioId);
    Optional<Estacion> findByMunicipioNameAndNameAndNumber(String municipioName, String name, String number);
}
