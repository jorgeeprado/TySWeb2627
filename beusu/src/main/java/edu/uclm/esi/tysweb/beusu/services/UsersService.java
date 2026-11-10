package edu.uclm.esi.tysweb.beusu.services;

import java.util.Map;
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
import edu.uclm.esi.tysweb.beusu.dto.ResetPasswordRequest;
import edu.uclm.esi.tysweb.beusu.dto.UsuarioValidadoDto;
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

    // googleAuth: GoogleAuthClient (atributo del diagrama)
    @Autowired
    private GoogleAuthClient googleAuth;

    // RT-CONF-01: Caducidad configurable del token de confirmación (24h)
    @Value("${app.token.confirmacion.expiration-ms:86400000}")
    private long tokenConfirmacionExpirationMs;

    // RT-CONF-02: Caducidad configurable e independiente del token de recuperación (30min)
    @Value("${app.token.recuperacion.expiration-ms:1800000}")
    private long tokenRecuperacionExpirationMs;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // =========================================================================
    // CASO DE USO: Registrar municipio
    // =========================================================================
    public Map<String, Object> registerMunicipio(RegistroRequest request) {
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
        user.setPagadoAlta(false);

        this.userDao.save(user);

        // 4. Enviar correo de confirmación
        this.emailService.enviarCorreoConfirmacion(user.getEmail(), user.getTokenCreacion());

        String confirmUrl = "http://localhost:8080/users/confirmar?token=" + user.getTokenCreacion();
        Map<String, Object> respuesta = new java.util.HashMap<>();
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

        User user = this.userDao.findByTokenCreacion(token.trim());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El token de confirmación no existe");
        }

        if (Boolean.TRUE.equals(user.getActivo())) {
            return user; // Ya confirmada: permitimos redirigir con éxito sin romper la experiencia
        }

        long tiempoTranscurrido = System.currentTimeMillis() - user.getHoraCreacion();
        if (tiempoTranscurrido > this.tokenConfirmacionExpirationMs) {
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
    // CASO DE USO: Solicitar recuperación de contraseña (RN-CUE-04, RC-SEC-03, RT-CONF-02)
    // Mensaje neutro obligatorio para evitar enumeración de usuarios
    // =========================================================================
    public Map<String, String> solicitarRecuperacionPwd(String email) {
        final String MENSAJE_NEUTRO = "Si el correo electrónico está registrado en el sistema, recibirás un mensaje con las instrucciones para restablecer tu contraseña.";

        if (email == null || email.trim().isEmpty()) {
            return Map.of("message", MENSAJE_NEUTRO);
        }

        String emailLimpio = email.trim().toLowerCase();
        User user = this.userDao.findByEmail(emailLimpio);

        // Solo si el usuario existe y está activo generamos token y enviamos correo
        if (user != null && Boolean.TRUE.equals(user.getActivo())) {
            // RN-CUE-04: Si existía una solicitud previa, se sobrescribe e invalida la anterior
            String tokenRecuperacion = UUID.randomUUID().toString();
            user.setTokenRecuperacion(tokenRecuperacion);
            user.setHoraRecuperacion(System.currentTimeMillis());
            this.userDao.save(user);

            // Enviar correo de forma asíncrona
            this.emailService.enviarCorreoRecuperacion(user.getEmail(), tokenRecuperacion);

            // Mensaje neutro idéntico (RC-SEC-03); incluimos el token en modo desarrollo por comodidad
            return Map.of(
                    "message", MENSAJE_NEUTRO,
                    "token", tokenRecuperacion
            );
        }

        // Si el usuario no existe o no está activo, respuesta neutra idéntica (RC-SEC-03)
        return Map.of("message", MENSAJE_NEUTRO);
    }

    // =========================================================================
    // CASO DE USO: Crear nueva contraseña con token de recuperación (RN-CUE-04, RT-CONF-02)
    // =========================================================================
    public Map<String, String> cambiarPasswordConToken(ResetPasswordRequest request) {
        if (request == null || request.token() == null || request.token().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de recuperación ausente");
        }

        if (request.nuevaPwd1() == null || request.nuevaPwd2() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las nuevas contraseñas no pueden ser nulas");
        }

        if (!request.nuevaPwd1().equals(request.nuevaPwd2())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las contraseñas no coinciden");
        }

        User user = this.userDao.findByTokenRecuperacion(request.token().trim());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "El token de recuperación no existe o ya ha sido utilizado");
        }

        long tiempoTranscurrido = System.currentTimeMillis() - user.getHoraRecuperacion();
        if (tiempoTranscurrido > this.tokenRecuperacionExpirationMs) {
            throw new ResponseStatusException(HttpStatus.GONE,
                    "El token de recuperación ha caducado. Por favor, solicita uno nuevo.");
        }

        // Actualizar contraseña hasheada
        user.setPassword(this.passwordEncoder.encode(request.nuevaPwd1()));
        // Invalidar el token para asegurar que sea de un solo uso (RN-CUE-04)
        user.setTokenRecuperacion(null);
        user.setHoraRecuperacion(null);
        this.userDao.save(user);

        this.emailService.enviarCorreoPasswordModificada(user.getEmail());

        return Map.of("message", "Contraseña restablecida con éxito. Ya puedes iniciar sesión con tu nueva contraseña.");
    }

    // =========================================================================
    // ENDPOINT INTERNO: Validación de sesión inter-backend (RT-AUT-02, RC-SEC-02)
    // =========================================================================
    public UsuarioValidadoDto validateSession(String authHeaderOrToken) {
        if (authHeaderOrToken == null || authHeaderOrToken.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cabecera de autorización o token ausente");
        }

        String token = authHeaderOrToken.trim();
        if (token.startsWith("Bearer ") || token.startsWith("bearer ")) {
            token = token.substring(7).trim();
        }

        if (!this.jwtUtil.esTokenValido(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token JWT inválido o caducado");
        }

        String email = this.jwtUtil.getEmailDelToken(token);
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token no contiene un sujeto válido");
        }

        User user = this.userDao.findByEmail(email);
        if (user == null || !Boolean.TRUE.equals(user.getActivo())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado o cuenta no activa");
        }

        return new UsuarioValidadoDto(
                user.getId(),
                user.getEmail(),
                user.getNombre(),
                user.getRol() != null ? user.getRol() : "EMPLEADO_AYTO",
                user.getMunicipio(),
                user.isPagadoAlta()
        );
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
    // =========================================================================
    public String activar2FA(String email) {
        User user = this.userDao.findByEmail(email);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }
        if (!Boolean.TRUE.equals(user.getActivo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta no está activa");
        }

        String secreto = this.googleAuth.createCredentials();
        String urlQr = this.googleAuth.getOtpAuthUrl(email, secreto);

        user.setSecretKey(secreto);
        this.userDao.save(user);

        return urlQr;
    }

    // =========================================================================
    // CASO DE USO: Verificar 2FA - Fase 2: Verificación sin red
    // =========================================================================
    public LoginResponse confirm2faCode(String email, int code) {
        User user = this.userDao.findByEmail(email);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }

        if (user.getSecretKey() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario no tiene 2FA configurado");
        }

        boolean esValido = this.googleAuth.authorize(user.getSecretKey(), code);
        if (!esValido) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Código 2FA inválido");
        }

        if (!user.isFa2Activated()) {
            user.setFa2Activated(true);
            this.userDao.save(user);
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

    public LoginResponse confirm2faCode(Map<String, Object> data) {
        String email = (String) data.get("email");
        int code = Integer.parseInt(data.get("code").toString());
        return confirm2faCode(email, code);
    }
}
