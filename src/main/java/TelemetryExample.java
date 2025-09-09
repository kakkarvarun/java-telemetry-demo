import io.micrometer.core.instrument.Gauge;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import java.util.Random;

public class TelemetryExample {
    public static void main(String[] args) throws InterruptedException {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(io.micrometer.prometheus.PrometheusConfig.DEFAULT);
        Random random = new Random();

        Gauge.builder("temperature_celsius", () -> random.nextInt(20, 30))
             .description("Temperature in Celsius")
             .register(registry);

        while (true) {
            System.out.println(registry.scrape());
            Thread.sleep(5000);
        }
    }
}
