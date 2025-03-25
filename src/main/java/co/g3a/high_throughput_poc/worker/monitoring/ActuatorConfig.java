package co.g3a.high_throughput_poc.worker.monitoring;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WebEndpointProperties.class)
public class ActuatorConfig {
    // La anotación @EnableConfigurationProperties activará las propiedades necesarias
    // Spring Boot se encargará automáticamente de registrar el endpoint
}