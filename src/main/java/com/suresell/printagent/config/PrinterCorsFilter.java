package com.suresell.printagent.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * CORS + Private Network Access para el agente local.
 *
 * La PWA se sirve por HTTPS desde un dominio público y llama al agente en
 * http://localhost. Chrome (Private Network Access) exige que el preflight a una
 * dirección local responda con `Access-Control-Allow-Private-Network: true`.
 * Sin esto, las llamadas de impresión se bloquean. Ver docs/20-agente-hardware.md §5.
 *
 * El agente solo hace hardware (no datos sensibles), por lo que refleja el Origin
 * solicitante. Restringir a orígenes concretos es posible vía configuración futura.
 */
@Component
public class PrinterCorsFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String origin = req.getHeader("Origin");
        if (origin != null) {
            res.setHeader("Access-Control-Allow-Origin", origin);
            res.setHeader("Vary", "Origin");
        }
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        res.setHeader("Access-Control-Max-Age", "3600");

        // Private Network Access: autorizar el acceso a red privada/local.
        if ("true".equalsIgnoreCase(req.getHeader("Access-Control-Request-Private-Network"))) {
            res.setHeader("Access-Control-Allow-Private-Network", "true");
        }

        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            res.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        chain.doFilter(req, res);
    }
}
