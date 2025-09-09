import io.micrometer.core.instrument.Gauge;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import java.util.Random;

// Tiny HTTP server
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class TelemetryExample {
    public static void main(String[] args) throws Exception {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(io.micrometer.prometheus.PrometheusConfig.DEFAULT);
        Random random = new Random();

        Gauge.builder("temperature_celsius", () -> 20 + random.nextInt(10))
             .description("Temperature in Celsius")
             .register(registry);

        // HTTP /metrics on port 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/metrics", http -> {
            String response = registry.scrape();
            byte[] bytes = response.getBytes();
            http.getResponseHeaders().add("Content-Type", "text/plain; version=0.0.4; charset=utf-8");
            http.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = http.getResponseBody()) { os.write(bytes); }
        });
        server.setExecutor(null);
        server.start();

        Thread.sleep(Long.MAX_VALUE); // keep running
    }
}
