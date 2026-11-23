package cl.qa.tickets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public final class ApiTickets {
    private static final String RUTA_PRIORIDAD = "/prioridad/";

    private ApiTickets() {
    }

    static HttpServer iniciar(int puerto, String version) throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", puerto), 0);
        servidor.createContext("/", intercambio -> atender(intercambio, version));
        servidor.start();
        return servidor;
    }

    private static void atender(HttpExchange intercambio, String version) throws IOException {
        try (intercambio) {
            if (!intercambio.getRequestMethod().equals("GET")) {
                intercambio.getResponseHeaders().set("Allow", "GET");
                responder(intercambio, 405, "método no permitido");
                return;
            }

            String ruta = intercambio.getRequestURI().getPath();
            if (ruta.equals("/salud")) {
                responder(intercambio, 200, version);
            } else if (ruta.startsWith(RUTA_PRIORIDAD)) {
                String prioridad;
                try {
                    prioridad = Prioridad.calcular(ruta.substring(RUTA_PRIORIDAD.length()));
                } catch (IllegalArgumentException invalido) {
                    responder(intercambio, 400, invalido.getMessage());
                    return;
                }
                responder(intercambio, 200, prioridad);
            } else {
                responder(intercambio, 404, "ruta inexistente");
            }
        }
    }

    private static void responder(HttpExchange intercambio, int codigo, String mensaje)
            throws IOException {
        byte[] cuerpo = mensaje.getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        intercambio.sendResponseHeaders(codigo, cuerpo.length);
        intercambio.getResponseBody().write(cuerpo);
    }

    public static void main(String[] args) throws IOException {
        int puerto = args.length > 0 ? Integer.parseInt(args[0]) : 18080;
        String version = args.length > 1 ? args[1] : "local";
        HttpServer servidor = iniciar(puerto, version);
        System.out.printf("Versión %s en http://127.0.0.1:%d%n", version, servidor.getAddress().getPort());
    }
}
