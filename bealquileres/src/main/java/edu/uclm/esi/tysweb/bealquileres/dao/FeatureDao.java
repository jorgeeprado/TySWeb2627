package edu.uclm.esi.tysweb.bealquileres.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.uclm.esi.tysweb.bealquileres.model.Feature;

public interface FeatureDao extends JpaRepository<Feature, String> {


    // Busca una calle exacta dentro de un municipio exacto
    Optional<Feature> findByMunicipioNameAndNameAndNumber(String municipioName, String name, String number);

    java.util.List<Feature> findByMunicipioName(String municipioName);

}