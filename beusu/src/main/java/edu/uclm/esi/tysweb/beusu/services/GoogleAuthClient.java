package edu.uclm.esi.tysweb.beusu.services;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import org.springframework.stereotype.Service;

/**
 * Servicio que encapsula la librería googleauth para la autenticación de dos factores (2FA).
 * Diagrama de clases: <<Service>> GoogleAuthClient
 *   - gAuth: GoogleAuthenticator
 *   + createCredentials(): String       -> genera el secreto y lo devuelve
 *   + getOtpAuthUrl(email, key): String -> genera la URL del QR para el Google Authenticator
 *   + authorize(secret, code): Boolean  -> verifica si el código TOTP es válido
 */
@Service
public class GoogleAuthClient {

    // gAuth: GoogleAuthenticator  (atributo del diagrama)
    private final GoogleAuthenticator gAuth;

    public GoogleAuthClient() {
        this.gAuth = new GoogleAuthenticator();
    }

    /**
     * createCredentials(): String
     * Genera una nueva clave secreta TOTP para el usuario.
     * Esta clave se almacena en el User (secretKey) y se usa para generar el QR.
     */
    public String createCredentials() {
        GoogleAuthenticatorKey key = this.gAuth.createCredentials();
        return key.getKey();
    }

    /**
     * getOtpAuthUrl(email, key): String
     * Genera la URL otpauth:// que el Google Authenticator lee mediante QR.
     * El issuer "EcoBikes Ayto" identifica la app en el móvil del usuario.
     */
    public String getOtpAuthUrl(String email, String secretKey) {
        return GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(
                "EcoBikes Ayto",  // issuer (nombre de la app en el móvil)
                email,            // account name (email del usuario)
                new GoogleAuthenticatorKey.Builder(secretKey).build()
        );
    }

    public String getOtpAuthURL(String email, String key) {
        return getOtpAuthUrl(email, key);
    }


    /**
     * authorize(secret, code): Boolean
     * Verifica si el código TOTP introducido por el usuario coincide con el secreto
     * almacenado, calculando el código con la misma hora. (Diagrama: Fase 2 de verificación)
     */
    public boolean authorize(String secret, int code) {
        return this.gAuth.authorize(secret, code);
    }
}