package com.udla.figuras.controlador;

import com.udla.figuras.servicio.CargadorGroovy;
import com.udla.figuras.servicio.FabricaFiguras;
import com.udla.figuras.servicio.RegistroFiguras;
import com.udla.figuras.servicio.ServicioFigurasExterno;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/figuras")
public class FiguraController {

    private final FabricaFiguras fabrica;
    private final RegistroFiguras registro;
    private final CargadorGroovy cargador;
    private final ServicioFigurasExterno externo;

    public FiguraController(FabricaFiguras fabrica, RegistroFiguras registro,
                            CargadorGroovy cargador, ServicioFigurasExterno externo) {
        this.fabrica = fabrica;
        this.registro = registro;
        this.cargador = cargador;
        this.externo = externo;
    }

    public record Peticion(String figura, Map<String, Object> parametros) { }

    /** Lista todas las figuras disponibles (nativas + Groovy) con sus parametros. */
    @GetMapping
    public List<Map<String, Object>> listar() {
        return registro.todas().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("nombre", e.nombre());
            m.put("origen", e.origen().name());
            m.put("parametros", fabrica.nombresDePropiedades(e.clase()));
            return m;
        }).toList();
    }

    /** POST {"figura":"Circulo","parametros":{"radio":5}} */
    @PostMapping("/crear")
    public FabricaFiguras.Resultado crear(@RequestBody Peticion p) {
        return fabrica.crearYCalcular(p.figura(), p.parametros());
    }

    /** GET /api/figuras/Circulo?radio=5  (forma rapida desde el navegador) */
    @GetMapping("/{figura}")
    public FabricaFiguras.Resultado crearPorUrl(@PathVariable String figura,
                                                @RequestParam Map<String, Object> parametros) {
        return fabrica.crearYCalcular(figura, parametros);
    }

    /** Inspecciona la clase en tiempo de ejecucion (atributos, metodos, constructores...). */
    @GetMapping("/{figura}/info")
    public Map<String, Object> info(@PathVariable String figura) {
        return fabrica.describir(figura);
    }

    /** Recibe codigo Groovy (texto plano) y lo compila como una figura nueva. */
    @PostMapping(value = "/groovy", consumes = {MediaType.TEXT_PLAIN_VALUE, MediaType.ALL_VALUE})
    public Map<String, Object> cargarGroovy(@RequestBody String codigo) {
        Class<?> c = cargador.cargar(codigo);
        return Map.of("mensaje", "Figura '" + c.getSimpleName() + "' compilada y registrada",
                "figura", fabrica.describir(c.getSimpleName()));
    }

    /** EXTRA: muestra lo que ofrece el servicio externo. */
    @GetMapping("/externas")
    public List<ServicioFigurasExterno.DefinicionRemota> verExternas(@RequestParam(required = false) String url) {
        return externo.consultar(url);
    }

    /** EXTRA: importa (descarga + compila) todas las figuras del servicio externo. */
    @PostMapping("/externas/importar")
    public Map<String, String> importarExternas(@RequestParam(required = false) String url) {
        return externo.importar(url);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> error(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(org.springframework.web.client.RestClientException.class)
    public ResponseEntity<Map<String, String>> errorExterno(RuntimeException e) {
        return ResponseEntity.status(502).body(Map.of("error", "No se pudo consultar el servicio externo: " + e.getMessage()));
    }
}
