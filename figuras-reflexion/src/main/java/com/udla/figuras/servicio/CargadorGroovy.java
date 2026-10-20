package com.udla.figuras.servicio;

import groovy.lang.GroovyClassLoader;
import jakarta.annotation.PostConstruct;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.stream.Stream;

/**
 * Compila codigo Groovy en tiempo de ejecucion y registra la clase resultante
 * como una figura mas. Desde ese momento la FabricaFiguras la trata igual que
 * a Circulo o Cuadrado, porque tambien la usa por reflexion.
 *
 * Las figuras cargadas se guardan en la carpeta "figuras-dinamicas/" y se
 * vuelven a compilar al iniciar la aplicacion.
 */
@Service
public class CargadorGroovy {

    private static final Logger log = LoggerFactory.getLogger(CargadorGroovy.class);
    private static final String NOMBRE_VALIDO = "[A-Za-z_][A-Za-z0-9_]*";

    private final RegistroFiguras registro;
    private final Path carpeta;
    private final GroovyClassLoader groovyLoader;

    public CargadorGroovy(RegistroFiguras registro,
                          @Value("${figuras.carpeta-dinamica:figuras-dinamicas}") String carpeta) {
        this.registro = registro;
        this.carpeta = Paths.get(carpeta);
        CompilerConfiguration config = new CompilerConfiguration();
        config.setSourceEncoding("UTF-8");
        // El padre es el classloader de la app: asi el codigo Groovy puede usar
        // "implements com.udla.figuras.modelo.Figura" y cualquier clase de Java.
        this.groovyLoader = new GroovyClassLoader(getClass().getClassLoader(), config);
    }

    /** Al arrancar, recompila todas las figuras Groovy guardadas anteriormente. */
    @PostConstruct
    void cargarGuardadas() throws IOException {
        Files.createDirectories(carpeta);
        try (Stream<Path> archivos = Files.list(carpeta)) {
            archivos.filter(p -> p.toString().endsWith(".groovy")).sorted().forEach(p -> {
                try {
                    Class<?> c = compilar(Files.readString(p, StandardCharsets.UTF_8));
                    log.info("Figura Groovy cargada al iniciar: {} ({})", c.getSimpleName(), p.getFileName());
                } catch (Exception e) {
                    log.warn("No se pudo cargar {}: {}", p.getFileName(), e.getMessage());
                }
            });
        }
    }

    /** Compila, valida, registra y guarda el codigo. Devuelve la clase creada. */
    public Class<?> cargar(String codigo) {
        Class<?> clase = compilar(codigo);
        guardar(clase.getSimpleName(), codigo);
        return clase;
    }

    private Class<?> compilar(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("El codigo Groovy esta vacio");
        }
        Class<?> clase;
        try {
            clase = groovyLoader.parseClass(codigo);
        } catch (CompilationFailedException e) {
            throw new IllegalArgumentException("Error de compilacion Groovy:\n" + e.getMessage());
        }
        validar(clase);
        registro.registrarGroovy(clase);
        return clase;
    }

    /** Usa reflexion para comprobar que la clase "se comporta" como figura. */
    private void validar(Class<?> clase) {
        if (groovy.lang.Script.class.isAssignableFrom(clase)) {
            throw new IllegalArgumentException("El codigo debe declarar una clase (class Pentagono { ... }), no un script suelto");
        }
        if (Modifier.isAbstract(clase.getModifiers()) || clase.isInterface()) {
            throw new IllegalArgumentException(clase.getSimpleName() + " no puede ser abstracta ni interfaz");
        }
        if (!clase.getSimpleName().matches(NOMBRE_VALIDO)) {
            throw new IllegalArgumentException("Nombre de clase no valido: " + clase.getSimpleName());
        }
        for (String metodo : new String[]{"area", "perimetro"}) {
            try {
                Method m = clase.getMethod(metodo);
                Class<?> r = m.getReturnType();
                if (!(r.isPrimitive() && r != void.class && r != boolean.class) && !Number.class.isAssignableFrom(r)
                        && r != Object.class) {
                    throw new IllegalArgumentException(metodo + "() debe devolver un numero");
                }
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException("La clase " + clase.getSimpleName()
                        + " debe tener el metodo publico " + metodo + "()");
            }
        }
        try {
            clase.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(clase.getSimpleName() + " necesita un constructor sin parametros");
        }
    }

    private void guardar(String nombre, String codigo) {
        try {
            Files.createDirectories(carpeta);
            Files.writeString(carpeta.resolve(nombre + ".groovy"), codigo, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("La figura {} se cargo pero no se pudo guardar en disco: {}", nombre, e.getMessage());
        }
    }
}
