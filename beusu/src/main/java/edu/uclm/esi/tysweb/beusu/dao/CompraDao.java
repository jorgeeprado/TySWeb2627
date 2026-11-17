package edu.uclm.esi.tysweb.beusu.dao;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.uclm.esi.tysweb.beusu.model.Compra;

@Repository
public interface CompraDao extends JpaRepository<Compra, Long> {

    Optional<Compra> findByIdTransaccion(String idTransaccion);

    List<Compra> findByUserEmail(String email);
}
