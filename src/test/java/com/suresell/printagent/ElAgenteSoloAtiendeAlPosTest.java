package com.suresell.printagent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.suresell.printagent.printer.PrinterPort;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * A10: el agente arrancado de verdad. Solo escucha en 127.0.0.1, solo atiende al POS por CORS, un lote tiene tope y
 * un error no enseña el detalle de la impresora. La impresora es un doble: nada sale a papel.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ElAgenteSoloAtiendeAlPosTest {

    private static final String POS = "https://pos-caja.suresell.com.co";
    private static final String AJENO = "https://evil.example.com";

    @LocalServerPort int puerto;
    @MockitoBean PrinterPort impresora;
    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void limpiar() {
        reset(impresora);
        when(impresora.isPrinterReady()).thenReturn(true);
    }

    private HttpResponse<String> pedir(String metodo, String ruta, String origen, String cuerpo, String... cabeceras) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + puerto + ruta))
                .method(metodo, cuerpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(cuerpo));
        if (origen != null) {
            b.header("Origin", origen);
        }
        if (cuerpo != null) {
            b.header("Content-Type", "application/json");
        }
        for (int i = 0; i < cabeceras.length; i += 2) {
            b.header(cabeceras[i], cabeceras[i + 1]);
        }
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("🔴 A10 · CORS: el POS recibe las cabeceras y red privada; una web ajena, 403 sin ninguna, y el cajón no se abre")
    void soloElPos() throws Exception {
        HttpResponse<String> preflightPos = pedir("OPTIONS", "/api/printer/drawer/open", POS, null,
                "Access-Control-Request-Method", "POST", "Access-Control-Request-Private-Network", "true");
        assertThat(preflightPos.statusCode()).isEqualTo(200);
        assertThat(preflightPos.headers().firstValue("Access-Control-Allow-Origin")).contains(POS);
        assertThat(preflightPos.headers().firstValue("Access-Control-Allow-Private-Network")).contains("true");

        HttpResponse<String> preflightAjeno = pedir("OPTIONS", "/api/printer/drawer/open", AJENO, null,
                "Access-Control-Request-Method", "POST", "Access-Control-Request-Private-Network", "true");
        assertThat(preflightAjeno.statusCode()).isEqualTo(403);
        assertThat(preflightAjeno.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
        assertThat(preflightAjeno.headers().firstValue("Access-Control-Allow-Private-Network")).isEmpty();

        // El POST «simple» que no pasa por preflight: el cajón no se abre para una web ajena.
        assertThat(pedir("POST", "/api/printer/drawer/open", AJENO, null).statusCode()).isEqualTo(403);
        verify(impresora, never()).openDrawer();
        // Por defecto, ni staging ni un programa local en el 4200: solo entran con AGENTE_ORIGENES.
        for (String fueraDelDefecto : new String[] {"https://posstaging.suresell.com.co", "http://localhost:4200"}) {
            HttpResponse<String> r = pedir("POST", "/api/printer/drawer/open", fueraDelDefecto, null);
            assertThat(r.statusCode()).as(fueraDelDefecto).isEqualTo(403);
            assertThat(r.headers().firstValue("Access-Control-Allow-Origin")).as(fueraDelDefecto).isEmpty();
        }
        verify(impresora, never()).openDrawer();
        assertThat(pedir("POST", "/api/printer/drawer/open", POS, null).statusCode()).as("control: el POS sí").isEqualTo(200);
        verify(impresora, times(1)).openDrawer();
        assertThat(pedir("GET", "/api/printer/status", null, null).statusCode()).as("sin Origin (la propia máquina) pasa").isEqualTo(200);
    }

    @Test
    @DisplayName("🔴 A10 · escucha solo en 127.0.0.1: por la IP de la red no hay conexión")
    void soloLaPropiaMaquina() throws Exception {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("127.0.0.1", puerto), 2000);
            assertThat(s.isConnected()).as("control: por 127.0.0.1 sí").isTrue();
        }
        Optional<InetAddress> deLaRed = Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
                .filter(i -> { try { return i.isUp() && !i.isLoopback(); } catch (Exception e) { return false; } })
                .flatMap(i -> Collections.list(i.getInetAddresses()).stream())
                .filter(a -> a instanceof Inet4Address && !a.isLoopbackAddress() && !a.isLinkLocalAddress())
                .findFirst();
        assertThat(deLaRed).as("hace falta una IP de red para medir; esta máquina no tiene ninguna").isPresent();
        assertThatThrownBy(() -> {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(deLaRed.get(), puerto), 2000);
            }
        }).as("por " + deLaRed.get().getHostAddress() + " no debe haber agente").isInstanceOf(java.io.IOException.class);
    }

    @Test
    @DisplayName("🔴 A10 · un lote de más de 20 tickets se rechaza (400) sin imprimir nada; 20 pasan")
    void topeDelLote() throws Exception {
        String ticket = """
                {"businessName":"Tienda","nit":"900","address":"Calle 1","phone":"300","ticketNumber":"1","cashierName":"Ana",
                 "dateTime":"2026-10-04 20:00","items":[],"subtotal":0,"tax":0,"total":0,"paymentMethod":"CASH"}""";
        String veintiuno = "[" + String.join(",", Collections.nCopies(21, ticket)) + "]";
        HttpResponse<String> r = pedir("POST", "/api/printer/ticket-batch", POS, veintiuno);
        assertThat(r.statusCode()).isEqualTo(400);
        verify(impresora, never()).printBytes(any());
        clearInvocations(impresora);
        String veinte = "[" + String.join(",", Collections.nCopies(20, ticket)) + "]";
        HttpResponse<String> ok = pedir("POST", "/api/printer/ticket-batch", POS, veinte);
        assertThat(ok.statusCode()).as(ok.body()).isEqualTo(200);
    }

    @Test
    @DisplayName("🔴 A10 · un 500 no enseña el detalle de la impresora ni de Java")
    void errorGenerico() throws Exception {
        doThrow(new RuntimeException("COM3: acceso denegado (javax.print interno)")).when(impresora).openDrawer();
        HttpResponse<String> r = pedir("POST", "/api/printer/drawer/open", POS, null);
        assertThat(r.statusCode()).isEqualTo(500);
        assertThat(r.body()).doesNotContain("COM3").doesNotContain("javax").contains("No se pudo abrir el cajón");
    }
}
