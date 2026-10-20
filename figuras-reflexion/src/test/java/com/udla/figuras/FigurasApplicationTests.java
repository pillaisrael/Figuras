package com.udla.figuras;

import com.udla.figuras.servicio.CargadorGroovy;
import com.udla.figuras.servicio.FabricaFiguras;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FigurasApplicationTests {

    @DynamicPropertySource
    static void carpetaTemporal(DynamicPropertyRegistry r) throws Exception {
        r.add("figuras.carpeta-dinamica", () -> {
            try { return Files.createTempDirectory("figuras-test").toString(); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
    }

    @Autowired FabricaFiguras fabrica;
    @Autowired CargadorGroovy cargador;
    @Autowired com.udla.figuras.servicio.ServicioFigurasExterno externo;
    @LocalServerPort int puerto;

    @Test
    void creaCirculoPorNombre() {
        var r = fabrica.crearYCalcular("Circulo", Map.of("radio", 5));
        assertEquals(78.5398, r.area(), 1e-4);
        assertEquals(31.4159, r.perimetro(), 1e-4);
        assertEquals("NATIVA", r.origen());
    }

    @Test
    void aceptaParametrosComoTexto() {
        var r = fabrica.crearYCalcular("rectangulo", Map.of("base", "4", "altura", "2.5"));
        assertEquals(10.0, r.area());
        assertEquals(13.0, r.perimetro());
    }

    @Test
    void trianguloConHeron() {
        var r = fabrica.crearYCalcular("Triangulo", Map.of("ladoA", 3, "ladoB", 4, "ladoC", 5));
        assertEquals(6.0, r.area());
        assertEquals(12.0, r.perimetro());
    }

    @Test
    void pentagonoGroovyFuncionaComoNativo() throws Exception {
        String codigo = Files.readString(Path.of("ejemplos-groovy/Pentagono.groovy"));
        cargador.cargar(codigo);
        var r = fabrica.crearYCalcular("Pentagono", Map.of("lado", 5));
        assertEquals("GROOVY", r.origen());
        assertEquals(43.0119, r.area(), 1e-4);
        assertEquals(25.0, r.perimetro());
    }

    @Test
    void importaDelServicioExterno() {
        var reporte = externo.importar("http://localhost:" + puerto + "/api/catalogo-remoto/figuras");
        assertEquals("cargada", reporte.get("Hexagono"));
        assertEquals("cargada", reporte.get("Elipse"));
        var elipse = fabrica.crearYCalcular("Elipse", Map.of("a", 3, "b", 2)); // sin implementar Figura
        assertEquals(18.8496, elipse.area(), 1e-4);
    }

    @Test
    void erroresClaros() {
        assertThrows(IllegalArgumentException.class, () -> fabrica.crearYCalcular("Estrella", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> fabrica.crearYCalcular("Circulo", Map.of("lado", 2)));
        assertThrows(IllegalArgumentException.class, () -> cargador.cargar("class Mala { }"));
    }
}
