package edu.uclm.esi.tysweb.beusu.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import edu.uclm.esi.tysweb.beusu.model.User;

public interface UserDao extends JpaRepository<User, String> {
    
    User findByEmail(String email);

    User findByTokenCreacion(String tokenCreacion);

    User findByTokenRecuperacion(String tokenRecuperacion);
}
