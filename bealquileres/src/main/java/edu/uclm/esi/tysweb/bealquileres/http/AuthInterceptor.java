package edu.uclm.esi.tysweb.bealquileres.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import edu.uclm.esi.tysweb.bealquileres.dto.UsuarioAutenticadoDto;
import edu.uclm.esi.tysweb.bealquileres.services.BeusuAuthClient;

import java.io.IOException;
import java.util.Optional;

/**
 * Interceptor de autenticación para bealquileres (RT-AUT-02, RC-SEC-02).
 * Verifica en cada petición protegida la validez de la sesión contra beusu.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private BeusuAuthClient beusuAuthClient;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Permitir solicitudes preflight de CORS sin autenticación
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        // Obtener el token de la cabecera Authorization o como parámetro (útil para SSE o descargas)
        String authHeader = request.getHeader("Authorization");
        String token = null;

        if (authHeader != null && !authHeader.isBlank()) {
            token = authHeader;
        } else {
            token = request.getParameter("token");
        }

        if (token == null || token.isBlank()) {
            sendUnauthorized(response, "Acceso no autorizado: cabecera Authorization o parámetro token ausente");
            return false;
        }

        // Consultar centralizadamente a beusu para validar el token (RT-AUT-02, RC-SEC-02)
        Optional<UsuarioAutenticadoDto> usuarioOpt = this.beusuAuthClient.validarToken(token);
        if (usuarioOpt.isEmpty()) {
            sendUnauthorized(response, "Sesión no válida, expirada o cuenta de usuario inactiva");
            return false;
        }

        UsuarioAutenticadoDto usuario = usuarioOpt.get();

        // Almacenar el usuario autenticado en los atributos de la petición para uso de controladores y auditoría
        request.setAttribute("usuarioAutenticado", usuario);
        request.setAttribute("authenticatedUser", usuario.email());

        return true;
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format("{\"error\": \"Unauthorized\", \"message\": \"%s\", \"status\": 401}", message));
    }
}
