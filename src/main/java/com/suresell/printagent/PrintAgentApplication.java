package com.suresell.printagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Agente de hardware SureSell (impresión térmica + cajón).
 *
 * Servicio local mínimo que corre en la máquina del cliente. Traduce peticiones
 * HTTP de la PWA en bytes ESC/POS hacia la impresora del sistema operativo
 * (clase SAT Q22 80mm). NO contiene lógica de negocio, datos ni sincronización.
 *
 * Ver docs/20-agente-hardware.md.
 */
@SpringBootApplication
public class PrintAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(PrintAgentApplication.class, args);
    }
}
