package cl.qa.tickets;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiTicketsIT {
    private static HttpServer servidor;
    private final HttpClient cliente = HttpClient.newHttpClient();

    @BeforeAll
    static void levantarServidor() throws IOException {
        servidor = ApiTickets.iniciar(0, "integracion");
    }

    @AfterAll
    static void detenerServidor() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    @Test
    void consultaPrioridadPorHttp() throws Exception {
        var respuesta = consultar("/prioridad/critico");
        assertEquals(200, respuesta.statusCode());
        assertEquals("alta", respuesta.body());
    }

    @Test
    void reportaImpactoInvalidoPorHttp() throws Exception {
        var respuesta = consultar("/prioridad/otro");
        assertEquals(400, respuesta.statusCode());
        assertEquals("impacto inválido", respuesta.body());
    }

    @Test
    void informaVersionActiva() throws Exception {
        var respuesta = consultar("/salud");
        assertEquals(200, respuesta.statusCode());
        assertEquals("integracion", respuesta.body());
    }

    private HttpResponse<String> consultar(String ruta) throws Exception {
        var uri = URI.create("http://127.0.0.1:" + servidor.getAddress().getPort() + ruta);
        var peticion = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(3)).GET().build();
        return cliente.send(peticion, HttpResponse.BodyHandlers.ofString());
    }
}
