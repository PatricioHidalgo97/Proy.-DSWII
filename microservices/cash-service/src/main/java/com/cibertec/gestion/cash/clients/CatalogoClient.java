package com.cibertec.gestion.cash.clients;

import com.cibertec.gestion.cash.clients.dto.BancoRemoto;
import com.cibertec.gestion.cash.clients.dto.PuestoRemoto;
import com.cibertec.gestion.cash.clients.dto.ServicioRemoto;
import com.cibertec.gestion.cash.clients.dto.SocioRemoto;
import com.cibertec.gestion.cash.exceptions.DependenciaNoDisponibleException;
import com.cibertec.gestion.cash.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.List;

/**
 * Cliente HTTP de catálogo (ESP-03 §6, §7, §8).
 *
 * <ul>
 *   <li>Reenvía el header {@code Authorization} del request actual: no usa
 *       credenciales técnicas ni tokens propios.</li>
 *   <li>Timeout corto y configurable por propiedad.</li>
 *   <li>Traduce 404 a {@link NotFoundException}, 401/403 al mismo código de
 *       error y timeout/conexión rechazada a
 *       {@link DependenciaNoDisponibleException} (503).</li>
 *   <li>No reintenta: un reintento silencioso podría duplicar documentos.</li>
 * </ul>
 */
@Component
public class CatalogoClient {

    private final RestClient restClient;

    public CatalogoClient(
            @Value("${app.clients.catalog.url}") String baseUrl,
            @Value("${app.clients.catalog.connect-timeout-ms:1500}") long connectTimeoutMs,
            @Value("${app.clients.catalog.read-timeout-ms:3000}") long readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    public ServicioRemoto getServicio(Long id) {
        ServicioRemoto servicio = get("/api/servicios/{id}", ServicioRemoto.class, id);
        if (servicio == null) throw new NotFoundException("Servicio no encontrado");
        return servicio;
    }

    public PuestoRemoto getPuesto(Long id) {
        PuestoRemoto puesto = get("/api/puestos/{id}", PuestoRemoto.class, id);
        if (puesto == null) throw new NotFoundException("Puesto no encontrado");
        return puesto;
    }

    public SocioRemoto getSocio(Long id) {
        SocioRemoto socio = get("/api/socios/{id}", SocioRemoto.class, id);
        if (socio == null) throw new NotFoundException("Socio no encontrado");
        return socio;
    }

    public BancoRemoto getBanco(Long id) {
        BancoRemoto banco = get("/api/bancos/{id}", BancoRemoto.class, id);
        if (banco == null) throw new NotFoundException("Banco no encontrado");
        return banco;
    }

    public List<PuestoRemoto> listPuestos() {
        return getList("/api/puestos", new ParameterizedTypeReference<List<PuestoRemoto>>() {
        });
    }

    public List<SocioRemoto> listSocios() {
        return getList("/api/socios", new ParameterizedTypeReference<List<SocioRemoto>>() {
        });
    }

    private <T> T get(String path, Class<T> type, Object... uriVariables) {
        try {
            return request(path, uriVariables).body(type);
        } catch (ResourceAccessException ex) {
            throw new DependenciaNoDisponibleException(
                    "El catálogo no está disponible; intente más tarde");
        }
    }

    private <T> List<T> getList(String path, ParameterizedTypeReference<List<T>> type) {
        try {
            return request(path).body(type);
        } catch (ResourceAccessException ex) {
            throw new DependenciaNoDisponibleException(
                    "El catálogo no está disponible; intente más tarde");
        }
    }

    private RestClient.ResponseSpec request(String path, Object... uriVariables) {
        RestClient.RequestHeadersSpec<?> spec = restClient.get().uri(path, uriVariables);
        String authorization = currentAuthorization();
        if (authorization != null) spec = spec.header("Authorization", authorization);
        return spec.retrieve()
                .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (req, res) -> {
                    throw new NotFoundException("Recurso no encontrado en catálogo");
                })
                .onStatus(status -> status.value() == HttpStatus.UNAUTHORIZED.value(), (req, res) -> {
                    throw new BadCredentialsException("No autorizado en catálogo");
                })
                .onStatus(status -> status.value() == HttpStatus.FORBIDDEN.value(), (req, res) -> {
                    throw new AccessDeniedException("No autorizado en catálogo");
                })
                .onStatus(status -> status.isError(), (req, res) -> {
                    throw new DependenciaNoDisponibleException("Catálogo no disponible");
                });
    }

    /**
     * Token del usuario que originó la petición (ESP-03 §7). Se reenvía tal cual.
     */
    private String currentAuthorization() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        return attributes.getRequest().getHeader("Authorization");
    }
}
