package com.suresell.printagent.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * CORS + Private Network Access para el agente local, SOLO para los orígenes del POS.
 *
 * <p>La PWA se sirve por HTTPS desde un dominio público y llama al agente en http://localhost. Chrome exige que el
 * preflight a una dirección local responda con {@code Access-Control-Allow-Private-Network: true}.
 *
 * <p>A10: antes reflejaba CUALQUIER Origin y concedía red privada a todos: cualquier web abierta en el equipo podía
 * imprimir o abrir el cajón. Ahora:
 * <ul>
 *   <li>un origen de la lista ({@code agente.origenes}) recibe las cabeceras de siempre;</li>
 *   <li>un origen ajeno recibe 403 y NINGUNA cabecera CORS, también en la petición real: un POST «simple» (sin cuerpo,
 *       como abrir el cajón) no pasa por preflight y se ejecutaría aunque el navegador no dejara leer la respuesta;</li>
 *   <li>sin cabecera Origin (curl desde la propia máquina), pasa: el agente ya solo escucha en 127.0.0.1.</li>
 * </ul>
 */
@Component
public class PrinterCorsFilter extends OncePerRequestFilter {

    private final Set<String> origenes;

    public PrinterCorsFilter(@Value("${agente.origenes}") String lista) {
        this.origenes = Arrays.stream(lista.split(",")).map(String::trim).filter(s -> !s.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        if (origenes.isEmpty()) {
            throw new IllegalStateException("agente.origenes (AGENTE_ORIGENES) está vacío: el POS no podría imprimir.");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String origin = req.getHeader("Origin");
        if (origin != null && !origenes.contains(origin)) {
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (origin != null) {
            res.setHeader("Access-Control-Allow-Origin", origin);
            res.setHeader("Vary", "Origin");
            res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
            res.setHeader("Access-Control-Max-Age", "3600");
            // Private Network Access: autorizar el acceso a red privada/local, solo al POS.
            if ("true".equalsIgnoreCase(req.getHeader("Access-Control-Request-Private-Network"))) {
                res.setHeader("Access-Control-Allow-Private-Network", "true");
            }
        }

        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            res.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        chain.doFilter(req, res);
    }
}
