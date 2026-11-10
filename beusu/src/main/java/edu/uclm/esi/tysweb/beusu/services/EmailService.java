package edu.uclm.esi.tysweb.beusu.services;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:no-reply@tysweb.es}")
    private String fromEmail;

    /**
     * Envía el correo de confirmación de registro con el token temporal (RN-CUE-01, RN-CUE-03)
     */
    public void enviarCorreoConfirmacion(String destinatario, String token) {
        String urlConfirmacion = "http://localhost:8080/users/confirmar?token=" + token;

        String asunto = "Confirmación de registro - Gestión de Bicicletas";
        String mensaje = """
                Hola,
                
                Te has registrado satisfactoriamente en el sistema de gestión de bicicletas eléctricas.
                Para activar tu cuenta de empleado municipal, por favor haz clic en el siguiente enlace:
                
                %s
                
                Este enlace de confirmación es temporal y caducará en 24 horas.
                """.formatted(urlConfirmacion);

        // Imprimir siempre en consola para facilidad en desarrollo/pruebas inmediatas
        System.out.println("================================================================================");
        System.out.println("[SISTEMA DE CORREO] Correo enviado a: " + destinatario);
        System.out.println("Asunto: " + asunto);
        System.out.println("Enlace de confirmación: " + urlConfirmacion);
        System.out.println("================================================================================");

        enviarPorSmtpAsincrono(destinatario, asunto, mensaje);
    }

    /**
     * Envía el correo informativo tras confirmar la cuenta (Caso de uso Confirmar cuenta, paso 4)
     */
    public void enviarCorreoCuentaConfirmada(String destinatario) {
        String asunto = "Cuenta confirmada correctamente - Gestión de Bicicletas";
        String mensaje = """
                Hola,
                
                Tu cuenta ha sido confirmada con éxito. Ya puedes iniciar sesión en el portal municipal.
                """;

        System.out.println("================================================================================");
        System.out.println("[SISTEMA DE CORREO] Cuenta activada para: " + destinatario);
        System.out.println("================================================================================");

        enviarPorSmtpAsincrono(destinatario, asunto, mensaje);
    }

    /**
     * Envía el correo para restablecer la contraseña con el token temporal (RN-CUE-04, RT-CONF-02)
     */
    public void enviarCorreoRecuperacion(String destinatario, String token) {
        String urlRecuperacion = "http://localhost:4200/reset-password?token=" + token;

        String asunto = "Recuperación de contraseña - Gestión de Bicicletas";
        String mensaje = """
                Hola,
                
                Has solicitado restablecer tu contraseña en el sistema de gestión de bicicletas eléctricas.
                Para crear una nueva contraseña, por favor haz clic en el siguiente enlace:
                
                %s
                
                Este enlace de recuperación es de un solo uso y caduca en 30 minutos.
                Si no has solicitado este cambio, puedes ignorar este mensaje de forma segura.
                """.formatted(urlRecuperacion);

        System.out.println("================================================================================");
        System.out.println("[SISTEMA DE CORREO] Correo de recuperación enviado a: " + destinatario);
        System.out.println("Asunto: " + asunto);
        System.out.println("Token de recuperación: " + token);
        System.out.println("Enlace de recuperación: " + urlRecuperacion);
        System.out.println("================================================================================");

        enviarPorSmtpAsincrono(destinatario, asunto, mensaje);
    }

    /**
     * Notificación de confirmación de cambio de contraseña completado
     */
    public void enviarCorreoPasswordModificada(String destinatario) {
        String asunto = "Contraseña actualizada - Gestión de Bicicletas";
        String mensaje = """
                Hola,
                
                Tu contraseña ha sido restablecida correctamente. Ya puedes acceder con tus nuevas credenciales.
                Si no has sido tú quien ha realizado este cambio, contacta inmediatamente con el administrador.
                """;

        System.out.println("================================================================================");
        System.out.println("[SISTEMA DE CORREO] Contraseña modificada para: " + destinatario);
        System.out.println("================================================================================");

        enviarPorSmtpAsincrono(destinatario, asunto, mensaje);
    }

    private void enviarPorSmtpAsincrono(String destinatario, String asunto, String texto) {
        if (this.mailSender == null) {
            log.warn("JavaMailSender no está configurado. El correo solo se muestra en consola.");
            return;
        }

        // Si son credenciales por defecto o de prueba, no intentar socket externo para evitar bloqueos
        if (this.fromEmail == null || this.fromEmail.contains("tu_correo") || this.fromEmail.contains("no-reply")) {
            log.info("Entorno local de desarrollo: correo registrado en consola. (SMTP omitido)");
            return;
        }

        // Ejecutar en hilo desacoplado para no bloquear NUNCA la respuesta HTTP al usuario
        CompletableFuture.runAsync(() -> {
            try {
                SimpleMailMessage mailMessage = new SimpleMailMessage();
                mailMessage.setFrom(this.fromEmail);
                mailMessage.setTo(destinatario);
                mailMessage.setSubject(asunto);
                mailMessage.setText(texto);

                this.mailSender.send(mailMessage);
                log.info("Correo enviado con éxito vía SMTP a {}", destinatario);
            } catch (Exception e) {
                log.warn("No se pudo enviar el correo vía SMTP a {}: {}. Usar enlace de consola.", 
                        destinatario, e.getMessage());
            }
        });
    }
}
