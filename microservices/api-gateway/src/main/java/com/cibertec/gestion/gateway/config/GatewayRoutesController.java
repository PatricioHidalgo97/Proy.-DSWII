package com.cibertec.gestion.gateway.config;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties;
import org.springframework.cloud.gateway.server.mvc.config.PredicateProperties;
import org.springframework.cloud.gateway.server.mvc.config.RouteProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Vista de solo lectura de las rutas activas (ESP-07 §7).
 *
 * Spring Cloud Gateway Server Web MVC 4.3.5 todavía no publica el endpoint
 * Actuator nativo {@code /actuator/gateway/routes} (issue spring-cloud-gateway
 * #3233). Este controlador expone la misma información a partir de
 * {@link GatewayMvcProperties}: id, uri, predicados y orden. No permite mutar
 * rutas y no contiene lógica de negocio.
 */
@RestController
@RequestMapping("/actuator/gateway")
public class GatewayRoutesController {

    private final GatewayMvcProperties properties;

    public GatewayRoutesController(GatewayMvcProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/routes")
    public List<Map<String, Object>> routes() {
        return properties.getRoutes().stream()
                .sorted(Comparator.comparingInt(RouteProperties::getOrder))
                .map(GatewayRoutesController::describe)
                .toList();
    }

    private static Map<String, Object> describe(RouteProperties route) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("route_id", route.getId());
        item.put("uri", route.getUri().toString());
        item.put("predicate", predicates(route.getPredicates()));
        item.put("order", route.getOrder());
        return item;
    }

    private static String predicates(List<PredicateProperties> predicates) {
        return predicates.stream()
                .map(predicate -> predicate.getName() + "=" + String.join(",", predicate.getArgs().values()))
                .reduce((left, right) -> left + " && " + right)
                .orElse("");
    }
}
