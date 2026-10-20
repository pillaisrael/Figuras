package com.udla.figuras.servicio;

import com.udla.figuras.modelo.Figura;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Catalogo de clases de figura conocidas por el sistema.
 *  - NATIVAS: se descubren escaneando el paquete del modelo (sin listarlas a mano).
 *  - GROOVY : se registran cuando se compila codigo Groovy en tiempo de ejecucion.
 */
@Component
public class RegistroFiguras {

    public static final String PAQUETE_MODELO = "com.udla.figuras.modelo";

    public enum Origen { NATIVA, GROOVY }

    public record Entrada(String nombre, Class<?> clase, Origen origen) { }

    private final Map<String, Entrada> figuras = new ConcurrentHashMap<>();

    public RegistroFiguras() {
        descubrirNativas();
    }

    /** Busca en el classpath todas las clases concretas que implementan Figura. */
    private void descubrirNativas() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(Figura.class));
        for (var bd : scanner.findCandidateComponents(PAQUETE_MODELO)) {
            try {
                Class<?> c = Class.forName(bd.getBeanClassName()); // reflexion: nombre -> clase
                figuras.put(clave(c.getSimpleName()), new Entrada(c.getSimpleName(), c, Origen.NATIVA));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public void registrarGroovy(Class<?> clase) {
        Entrada existente = figuras.get(clave(clase.getSimpleName()));
        if (existente != null && existente.origen() == Origen.NATIVA) {
            throw new IllegalArgumentException("'" + clase.getSimpleName()
                    + "' ya existe como figura nativa y no se puede reemplazar");
        }
        figuras.put(clave(clase.getSimpleName()), new Entrada(clase.getSimpleName(), clase, Origen.GROOVY));
    }

    public Optional<Entrada> buscar(String nombre) {
        return Optional.ofNullable(figuras.get(clave(nombre)));
    }

    public List<Entrada> todas() {
        List<Entrada> lista = new ArrayList<>(figuras.values());
        lista.sort(Comparator.comparing(Entrada::origen).thenComparing(Entrada::nombre));
        return lista;
    }

    private static String clave(String nombre) {
        return nombre.trim().toLowerCase(Locale.ROOT);
    }
}
