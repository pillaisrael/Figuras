package com.udla.figuras.servicio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * EXTRA: consume un servicio externo (HTTP/JSON) que publica nuevas figuras
 * escritas en Groovy y las compila con el CargadorGroovy.
 *
 * Formato esperado del servicio:
 * [ { "nombre": "Hexagono", "descripcion": "...", "codigo": "class Hexagono { ... }" }, ... ]
 *
 * Por defecto apunta al servicio simulado de esta misma app
 * (/api/catalogo-remoto/figuras), pero se puede cambiar por cualquier URL en
 * application.properties o enviando ?url=... en la peticion.
 */
@Service
public class ServicioFigurasExterno {

    public record DefinicionRemota(String nombre, String descripcion, String codigo) { }

    private final CargadorGroovy cargador;
    private final RestClient http;
    private final String urlPorDefecto;

    public ServicioFigurasExterno(CargadorGroovy cargador,
                                  RestClient.Builder builder,
                                  @Value("${figuras.servicio-externo.url}") String urlPorDefecto) {
        this.cargador = cargador;
        this.http = builder.build();
        this.urlPorDefecto = urlPorDefecto;
    }

    /** Consulta el catalogo remoto sin cargar nada. */
    public List<DefinicionRemota> consultar(String url) {
        List<DefinicionRemota> lista = http.get()
                .uri(url == null || url.isBlank() ? urlPorDefecto : url)
                .retrieve()
                .body(new ParameterizedTypeReference<List<DefinicionRemota>>() { });
        return lista == null ? List.of() : lista;
    }

    /** Descarga todas las figuras del servicio y las compila. Devuelve un reporte por figura. */
    public Map<String, String> importar(String url) {
        Map<String, String> reporte = new LinkedHashMap<>();
        for (DefinicionRemota d : consultar(url)) {
            try {
                Class<?> c = cargador.cargar(d.codigo());
                reporte.put(c.getSimpleName(), "cargada");
            } catch (Exception e) {
                reporte.put(d.nombre(), "error: " + e.getMessage());
            }
        }
        return reporte;
    }
}
