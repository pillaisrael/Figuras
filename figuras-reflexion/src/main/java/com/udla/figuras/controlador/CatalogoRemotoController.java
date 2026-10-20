package com.udla.figuras.controlador;

import com.udla.figuras.servicio.ServicioFigurasExterno.DefinicionRemota;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * SERVICIO EXTERNO SIMULADO.
 * Representa a un proveedor remoto de figuras. En un caso real estaria en otro
 * servidor; aqui vive en la misma app para que la demo funcione sin internet.
 * Sirve los archivos de src/main/resources/catalogo-remoto/*.groovy como JSON.
 */
@RestController
public class CatalogoRemotoController {

    @GetMapping("/api/catalogo-remoto/figuras")
    public List<DefinicionRemota> figuras() throws IOException {
        Resource[] archivos = new PathMatchingResourcePatternResolver()
                .getResources("classpath:catalogo-remoto/*.groovy");
        List<DefinicionRemota> lista = new ArrayList<>();
        for (Resource r : archivos) {
            String nombre = r.getFilename().replace(".groovy", "");
            String codigo = r.getContentAsString(StandardCharsets.UTF_8);
            String descripcion = codigo.lines().findFirst().orElse("").replaceFirst("^//\\s*", "");
            lista.add(new DefinicionRemota(nombre, descripcion, codigo));
        }
        return lista;
    }
}
