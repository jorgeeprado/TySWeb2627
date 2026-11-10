package edu.uclm.esi.tysweb.beusu.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import edu.uclm.esi.tysweb.beusu.model.User;
import java.util.Optional;

public interface UserDao extends JpaRepository<User, String> {
    
    // Has hecho muy bien en conservar esta función.
    // Spring Boot generará mágicamente la consulta SQL "SELECT * FROM user WHERE email = ?"
    User findByEmail(String email);

    User findByTokenCreacion(String tokenCreacion);
}