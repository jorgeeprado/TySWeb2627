package edu.uclm.esi.tysweb.bealquileres.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Interceptor de registro de operaciones para bealquileres.
 * Cumplimiento de especificaciones:
 *   - RT-LOG-01: Interceptación y registro mediante HandlerInterceptor de Spring MVC.
 *   - RC-AUD-01: Registro de timestamp, método, URI, IP cliente, código HTTP y duración en ms.
 *   - RC-AUD-02: Identificación del usuario o 'ANONYMOUS'.
 *   - RC-SEC-05: Prohibición estricta de registrar contraseñas, tokens completos o secretos.
 */
@Component
public class AuditInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger("edu.uclm.esi.tysweb.AUDIT");
    private static final String START_TIME_ATTR = "AUDIT_START_TIME";

    // Palabras reservadas sensibles que deben enmascararse en URI/parámetros (RC-SEC-05)
    private static final List<String> SENSITIVE_KEYWORDS = List.of(
            "pwd", "password", "token", "secret", "authorization", "code", "card", "cvv", "key"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
        long durationMs = (startTime != null) ? (System.currentTimeMillis() - startTime) : 0;

        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        String method = request.getMethod();
        String uri = sanitizeUri(request);
        String clientIp = resolveClientIp(request);
        int status = response.getStatus();
        String user = resolveUser(request);

        log.info("[AUDIT-ALQUILERES] Timestamp={} | Method={} | URI={} | Status={} | Duration={}ms | IP={} | User={}",
                timestamp, method, uri, status, durationMs, clientIp, user);
    }

    private String sanitizeUri(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        if (queryString == null || queryString.isBlank()) {
            return uri;
        }

        String[] pairs = queryString.split("&");
        StringBuilder sanitized = new StringBuilder();
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            String key = kv[0];
            String lowerKey = key.toLowerCase();
            boolean isSensitive = SENSITIVE_KEYWORDS.stream().anyMatch(lowerKey::contains);

            if (sanitized.length() > 0) {
                sanitized.append("&");
            }
            if (isSensitive) {
                sanitized.append(key).append("=[PROTECTED]");
            } else {
                sanitized.append(pair);
            }
        }
        return uri + "?" + sanitized;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveUser(HttpServletRequest request) {
        Object userAttr = request.getAttribute("authenticatedUser");
        if (userAttr != null) {
            return userAttr.toString();
        }
        Object usuarioObj = request.getAttribute("usuarioAutenticado");
        if (usuarioObj != null) {
            return usuarioObj.toString();
        }
        if (request.getUserPrincipal() != null) {
            return request.getUserPrincipal().getName();
        }
        return "ANONYMOUS";
    }
}
