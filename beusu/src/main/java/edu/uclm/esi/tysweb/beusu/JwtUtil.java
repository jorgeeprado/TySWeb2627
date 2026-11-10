package edu.uclm.esi.tysweb.beusu;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

// @Component le dice a Spring que esta clase es un "Bean" (un componente).
// Esto significa que Spring creará una instancia automáticamente y la 
// inyectará donde la necesitemos (como en nuestro Controlador) usando @Autowired.
@Component
public class JwtUtil {
    
    // Generamos una clave secreta muy segura usando el algoritmo HS256. 
    // Esta clave es como el "sello oficial" del servidor. Si alguien intenta
    // modificar un token en el front-end, el servidor lo rechazará porque
    // la firma no coincidirá con esta clave secreta.
    private static final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    
    // Tiempo de vida del token en milisegundos (86400000 ms = 24 horas).
    // Después de un día, el usuario tendrá que volver a hacer login.
    private static final long EXPIRATION_TIME = 86400000; 

    public String generarToken(String email, String rol) {
        return Jwts.builder()
                .setSubject(email) // El "sujeto" principal del token (quién es)
                .claim("rol", rol) // Añadimos información extra (el rol del usuario)
                .setIssuedAt(new Date()) // Fecha exacta de creación
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // Fecha de caducidad
                .signWith(SECRET_KEY) // Firmamos el token con nuestro "sello oficial"
                .compact(); // Construimos el JWT final y lo convertimos a un String (texto)
    }
}