package edu.uclm.esi.tysweb.beusu.services;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.esi.tysweb.beusu.JwtUtil;
import edu.uclm.esi.tysweb.beusu.dao.UserDao;
import edu.uclm.esi.tysweb.beusu.dto.LoginRequest;
import edu.uclm.esi.tysweb.beusu.dto.LoginResponse;
import edu.uclm.esi.tysweb.beusu.dto.RegistroRequest;
import edu.uclm.esi.tysweb.beusu.model.User;

@Service
public class UsersService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private IdeeClient ideeClient;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtUtil jwtUtil;

    // googleAuth: GoogleAuthClient  (atributo del diagrama)
    @Autowired
    private GoogleAuthClient googleAuth;

    @Value("${app.token.confirmacion.expiration-ms:86400000}")
    private long tokenExpirationMs;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // =========================================================================
    // CASO DE USO: Registrar municipio
    // =========================================================================
    public java.util.Map<String, Object> registerMunicipio(RegistroRequest request) {
        String emailLimpio = request.email().trim().toLowerCase();

        // 1. Comprobar si ya existe una cuenta con este email
        User userExistente = this.userDao.findByEmail(emailLimpio);
        if (userExistente != null && Boolean.TRUE.equals(userExistente.getActivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya tiene una cuenta activa");
        }

        // 2. Validar con idee.es si el municipio existe (RN-CUE-02 / Escenario alternativo 3)
        if (!this.ideeClient.existsMunicipio(request.municipio())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El municipio indicado no existe");
        }

        // 3. Preparar la entidad de usuario (RN-CUE-05: si existía sin activar, sustituye previa)
        User user = (userExistente != null) ? userExistente : new User();
        if (user.getId() == null) {
            user.setId(UUID.randomUUID().toString());
        }

        user.setEmail(emailLimpio);
        user.setNombre(request.nombre());
        user.setApellidos(request.apellidos());
        user.setMunicipio(request.municipio());
        user.setLatitud(request.latitud());
        user.setLongitud(request.longitud());

        user.setPassword(this.passwordEncoder.encode(request.psw()));
        user.setActivo(false);
        user.setTokenCreacion(UUID.randomUUID().toString());
        user.setHoraCreacion(System.currentTimeMillis());
        user.setHoraConfirmacion(null);
        user.setRol("EMPLEADO_AYTO");

        this.userDao.save(user);

        // 4. Enviar correo de confirmación
        this.emailService.enviarCorreoConfirmacion(user.getEmail(), user.getTokenCreacion());

        String confirmUrl = "http://localhost:8080/users/confirmar?token=" + user.getTokenCreacion();
        java.util.Map<String, Object> respuesta = new java.util.HashMap<>();
        respuesta.put("message", "Registro completado con éxito. Por favor confirma tu cuenta.");
        respuesta.put("email", user.getEmail());
        respuesta.put("token", user.getTokenCreacion());
        respuesta.put("confirmUrl", confirmUrl);
        return respuesta;
    }

    // =========================================================================
    // CASO DE USO: Confirmar cuenta (RN-CUE-01, RN-CUE-03, RT-CONF-01)
    // =========================================================================
    public User confirmarCuenta(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de confirmación ausente");
        }

        User user = this.userDao.findByTokenCreacion(token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El token de confirmación no existe");
        }

        if (Boolean.TRUE.equals(user.getActivo())) {
            return user; // Ya confirmada: permitimos redirigir con éxito sin romper la experiencia
        }

        long tiempoTranscurrido = System.currentTimeMillis() - user.getHoraCreacion();
        if (tiempoTranscurrido > this.tokenExpirationMs) {
            throw new ResponseStatusException(HttpStatus.GONE,
                    "El token de confirmación ha caducado. Debe iniciar de nuevo el proceso de registro");
        }

        user.setActivo(true);
        user.setHoraConfirmacion(System.currentTimeMillis());
        this.userDao.save(user);

        this.emailService.enviarCorreoCuentaConfirmada(user.getEmail());

        return user;
    }

    // =========================================================================
    // Activación directa / Rescate de cuenta en desarrollo
    // =========================================================================
    public User activarDirecto(String email, String token) {
        User user = null;
        if (token != null && !token.trim().isEmpty()) {
            user = this.userDao.findByTokenCreacion(token.trim());
        }
        if (user == null && email != null && !email.trim().isEmpty()) {
            user = this.userDao.findByEmail(email.trim().toLowerCase());
        }
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró ningún usuario pendiente de activar");
        }
        user.setActivo(true);
        user.setHoraConfirmacion(System.currentTimeMillis());
        this.userDao.save(user);
        this.emailService.enviarCorreoCuentaConfirmada(user.getEmail());
        return user;
    }

    // =========================================================================
    // CASO DE USO: Login (RN-CUE-01)
    // =========================================================================
    public LoginResponse login(LoginRequest request) {
        if (request == null || request.email() == null || request.pwd() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credenciales incompletas");
        }

        User user = this.userDao.findByEmail(request.email().trim());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        }

        if (!this.passwordEncoder.matches(request.pwd(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        }

        if (!Boolean.TRUE.equals(user.getActivo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "La cuenta no ha sido activada aún. Por favor, revisa tu correo para confirmar la cuenta.");
        }

        String token = this.jwtUtil.generarToken(user.getEmail(), user.getRol() != null ? user.getRol() : "EMPLEADO_AYTO");

        return new LoginResponse(
                token,
                user.getEmail(),
                user.getNombre(),
                user.getMunicipio(),
                user.getRol(),
                user.isFa2Activated()
        );
    }

    // =========================================================================
    // CASO DE USO: Activar 2FA - Fase 1: Generar y compartir el secreto
    // Flujo del diagrama: pasos 2-10
    //   activar2FA(email): String
    // =========================================================================
    public String activar2FA(String email) {
        // Paso 3: Buscar usuario
        User user = this.userDao.findByEmail(email);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }
        if (!Boolean.TRUE.equals(user.getActivo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta no está activa");
        }

        // Paso 5: Crear secreto (mediante GoogleAuthClient)
        String secreto = this.googleAuth.createCredentials();

        // Paso 7: Generar URL del QR
        String urlQr = this.googleAuth.getOtpAuthUrl(email, secreto);

        // Paso 9: Guardar secreto del usuario en BBDD
        user.setSecretKey(secreto);
        this.userDao.save(user);

        // Paso 10-11: Devolver URL del QR al frontend para que muestre el código
        return urlQr;
    }

    // =========================================================================
    // CASO DE USO: Verificar 2FA - Fase 2: Verificación sin red
    // Flujo del diagrama: pasos 17-37
    //   confirm2faCode(data): token JWT
    // =========================================================================
    public LoginResponse confirm2faCode(String email, int code) {
        // Paso 19: Buscar usuario con su secreto
        User user = this.userDao.findByEmail(email);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }

        if (user.getSecretKey() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario no tiene 2FA configurado");
        }

        // Paso 21-23: Calcula el código con el mismo secreto y hora, y compara
        boolean esValido = this.googleAuth.authorize(user.getSecretKey(), code);
        if (!esValido) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Código 2FA inválido");
        }

        // Paso 25: Marcar 2FA como activo (si viene de la activación inicial)
        if (!user.isFa2Activated()) {
            user.setFa2Activated(true);
            this.userDao.save(user);
        }

        // Paso 26: Crear token de sesión
        String token = this.jwtUtil.generarToken(user.getEmail(), user.getRol() != null ? user.getRol() : "EMPLEADO_AYTO");

        // Paso 27: OK + token
        return new LoginResponse(
                token,
                user.getEmail(),
                user.getNombre(),
                user.getMunicipio(),
                user.getRol(),
                user.isFa2Activated()
        );
    }

    public LoginResponse confirm2faCode(java.util.Map<String, Object> data) {
        String email = (String) data.get("email");
        int code = Integer.parseInt(data.get("code").toString());
        return confirm2faCode(email, code);
    }
}