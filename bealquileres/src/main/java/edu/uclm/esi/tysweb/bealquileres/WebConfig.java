package edu.uclm.esi.tysweb.bealquileres;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import edu.uclm.esi.tysweb.bealquileres.http.AuditInterceptor;
import edu.uclm.esi.tysweb.bealquileres.http.AuthInterceptor;

/**
 * Configuración MVC de bealquileres:
 * - Registro de interceptor de auditoría (RT-LOG-01, RC-AUD-01, RC-AUD-02, RC-SEC-05)
 * - Registro de interceptor de autenticación con beusu (RT-AUT-02, RC-SEC-02)
 * - Configuración de CORS para el frontend
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuditInterceptor auditInterceptor;

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Auditoría transversal para todas las rutas
        registry.addInterceptor(auditInterceptor)
                .addPathPatterns("/**");

        // Autenticación obligatoria contra beusu en operaciones de la API
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/vehiculos/health",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/error"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
                .allowedHeaders("*")
                .allowCredentials(false);
    }
}
