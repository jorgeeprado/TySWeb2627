package edu.uclm.esi.tysweb.beusu.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.uclm.esi.tysweb.beusu.dao.UserDao;
import edu.uclm.esi.tysweb.beusu.dto.RegistroRequest;
import edu.uclm.esi.tysweb.beusu.model.User;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
public class TestRegisterClase {

    @Autowired
    private MockMvc server;

    @Autowired
    private UserDao userDao;

    @Test
    void testRegistrarNormal() throws Exception {

        RegistroRequest body = new RegistroRequest(
                "Pepe", "Pérez Pérez", "pepe@pepe.es",
                "Horcajo de los Montes", 39.3269073, -4.6507151,
                "Pepe123!", "Pepe123!");

        RequestBuilder request = MockMvcRequestBuilders.put("/users/registerMunicipio")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(body)); 

        this.server.perform(request).andExpect(status().isOk());

        User user = this.userDao.findByEmail("pepe@pepe.es");
        assertNotNull(user);
        assertNotEquals("Pepe123!", user.getPassword());
        assertTrue(new BCryptPasswordEncoder().matches("Pepe123!", user.getPassword()));
        assertFalse(user.getActivo());

        assertNotNull(user.getTokenCreacion());
        assertNotNull(user.getHoraCreacion());
        assertNull(user.getHoraConfirmacion());
    }

    @Test
    void testRechazaMunicipioInexistente() throws Exception {
        RegistroRequest body = new RegistroRequest(
                "Pepe", "Pérez Pérez", "pepe_inexistente@pepe.es",
                "MunicipioInventadoQueNoExiste999", 39.3269073, -4.6507151,
                "Pepe123!", "Pepe123!");

        RequestBuilder request = MockMvcRequestBuilders.put("/users/registerMunicipio")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(body));

        this.server.perform(request).andExpect(status().isNotFound());
    }

    private void assertInvalidRegistration(String json) throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.put("/users/registerMunicipio")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);
        this.server.perform(request).andExpect(status().isBadRequest());
    }

    @Test
    void testRechazaCamposObligatoriosAusentes() throws Exception {
        assertInvalidRegistration("{}");
    }

    @Test
    void testRechazaContrasenasInvalidas() throws Exception {
        RegistroRequest body = new RegistroRequest(
                "Pepe", "Pérez Pérez", "pepe2@pepe.es",
                "Horcajo de los Montes", 39.3269073, -4.6507151,
                "Pepe123!", "OtraCosa123!");
        assertInvalidRegistration(new ObjectMapper().writeValueAsString(body));
    }

    @Test
    void testConfirmarCuentaExitoso() throws Exception {
        User user = new User();
        user.setId(java.util.UUID.randomUUID().toString());
        user.setEmail("confirmar@pepe.es");
        user.setNombre("Pepe");
        user.setApellidos("Perez");
        user.setPassword("hashedPwd");
        user.setActivo(false);
        String token = java.util.UUID.randomUUID().toString();
        user.setTokenCreacion(token);
        user.setHoraCreacion(System.currentTimeMillis());
        this.userDao.save(user);

        this.server.perform(MockMvcRequestBuilders.get("/users/confirmar").param("token", token))
                .andExpect(status().is3xxRedirection());

        User confirmado = this.userDao.findByTokenCreacion(token);
        assertNotNull(confirmado);
        assertTrue(confirmado.getActivo());
        assertNotNull(confirmado.getHoraConfirmacion());
    }
}
