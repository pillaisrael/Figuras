package com.udla.figuras.servicio;

import org.springframework.stereotype.Service;

import java.lang.reflect.*;
import java.util.*;

/**
 * Corazon de la tarea: crea cualquier figura a partir de su NOMBRE y de un mapa
 * de PARAMETROS, todo con reflexion. No hay ningun "new Circulo()" ni "switch".
 *
 *   ("Circulo", {radio=5})  ->  Class -> constructor -> setRadio(5.0) -> area(), perimetro()
 */
@Service
public class FabricaFiguras {

    private final RegistroFiguras registro;

    public FabricaFiguras(RegistroFiguras registro) {
        this.registro = registro;
    }

    public record Resultado(String figura, String origen, String clase,
                            Map<String, Object> parametros, double area, double perimetro) { }

    /** 1) Nombre -> clase, 2) instancia, 3) inyecta parametros, 4) invoca area/perimetro. */
    public Resultado crearYCalcular(String nombre, Map<String, Object> parametros) {
        RegistroFiguras.Entrada entrada = registro.buscar(nombre)
                .orElseThrow(() -> new IllegalArgumentException("La figura '" + nombre + "' no existe. "
                        + "Disponibles: " + registro.todas().stream().map(RegistroFiguras.Entrada::nombre).toList()));

        Object figura = instanciar(entrada.clase(), parametros == null ? Map.of() : parametros);

        double area = invocarNumero(figura, "area");
        double perimetro = invocarNumero(figura, "perimetro");

        return new Resultado(entrada.nombre(), entrada.origen().name(), entrada.clase().getName(),
                leerPropiedades(figura), redondear(area), redondear(perimetro));
    }

    /** Crea el objeto con el constructor vacio y asigna cada parametro por reflexion. */
    public Object instanciar(Class<?> clase, Map<String, Object> parametros) {
        try {
            Constructor<?> constructor = clase.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object instancia = constructor.newInstance();

            for (Map.Entry<String, Object> p : parametros.entrySet()) {
                asignar(instancia, p.getKey(), p.getValue());
            }
            return instancia;
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(clase.getSimpleName() + " necesita un constructor sin parametros");
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Error dentro de la figura: " + e.getTargetException().getMessage());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("No se pudo instanciar " + clase.getSimpleName() + ": " + e.getMessage());
        }
    }

    /** Primero intenta con el setter (setRadio); si no hay, escribe el atributo directamente. */
    private void asignar(Object obj, String propiedad, Object valor) throws ReflectiveOperationException {
        String setter = "set" + Character.toUpperCase(propiedad.charAt(0)) + propiedad.substring(1);

        for (Method m : obj.getClass().getMethods()) {
            if (m.getName().equals(setter) && m.getParameterCount() == 1) {
                m.invoke(obj, convertir(valor, m.getParameterTypes()[0], propiedad));
                return;
            }
        }
        Field campo = buscarCampo(obj.getClass(), propiedad);
        if (campo == null) {
            throw new IllegalArgumentException("La figura " + obj.getClass().getSimpleName()
                    + " no tiene el parametro '" + propiedad + "'. Parametros validos: "
                    + nombresDePropiedades(obj.getClass()));
        }
        campo.setAccessible(true);
        campo.set(obj, convertir(valor, campo.getType(), propiedad));
    }

    /** Ejecuta un metodo sin argumentos por nombre y devuelve su valor numerico. */
    private double invocarNumero(Object obj, String metodo) {
        try {
            Method m = obj.getClass().getMethod(metodo);
            Object r = m.invoke(obj);
            if (!(r instanceof Number n)) {
                throw new IllegalStateException(metodo + "() debe devolver un numero");
            }
            return n.doubleValue();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(obj.getClass().getSimpleName() + " no define el metodo " + metodo + "()");
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Error al calcular " + metodo + ": " + e.getTargetException().getMessage());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Lee de vuelta los valores con los getters, para mostrar que la instancia quedo bien armada. */
    private Map<String, Object> leerPropiedades(Object obj) {
        Map<String, Object> props = new LinkedHashMap<>();
        for (String nombre : nombresDePropiedades(obj.getClass())) {
            try {
                Field f = buscarCampo(obj.getClass(), nombre);
                f.setAccessible(true);
                props.put(nombre, f.get(obj));
            } catch (Exception ignorado) { /* propiedad no legible */ }
        }
        return props;
    }

    /** Atributos propios de la figura (sin estaticos ni sinteticos que agrega Groovy). */
    public List<String> nombresDePropiedades(Class<?> clase) {
        List<String> nombres = new ArrayList<>();
        for (Class<?> c = clase; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers()) && !f.isSynthetic()
                        && !f.getName().startsWith("$") && !f.getName().equals("metaClass")) {
                    nombres.add(f.getName());
                }
            }
        }
        return nombres;
    }

    /** "Inspeccion en tiempo de ejecucion": que hay dentro de una clase. */
    public Map<String, Object> describir(String nombre) {
        RegistroFiguras.Entrada e = registro.buscar(nombre)
                .orElseThrow(() -> new IllegalArgumentException("La figura '" + nombre + "' no existe"));
        Class<?> c = e.clase();

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("nombre", c.getSimpleName());
        info.put("claseCompleta", c.getName());
        info.put("origen", e.origen().name());
        info.put("superclase", c.getSuperclass() == null ? null : c.getSuperclass().getName());
        info.put("interfaces", Arrays.stream(c.getInterfaces()).map(Class::getName).toList());
        info.put("parametros", nombresDePropiedades(c).stream()
                .map(n -> n + " : " + buscarCampo(c, n).getType().getSimpleName()).toList());
        info.put("constructores", Arrays.stream(c.getDeclaredConstructors()).map(Constructor::toGenericString).toList());
        info.put("metodos", Arrays.stream(c.getDeclaredMethods())
                .filter(m -> !m.isSynthetic() && !m.getName().contains("$"))
                .map(m -> m.getReturnType().getSimpleName() + " " + m.getName() + "("
                        + String.join(", ", Arrays.stream(m.getParameterTypes()).map(Class::getSimpleName).toList()) + ")")
                .sorted().toList());
        info.put("classLoader", c.getClassLoader() == null ? "bootstrap" : c.getClassLoader().getClass().getName());
        return info;
    }

    private static Field buscarCampo(Class<?> clase, String nombre) {
        for (Class<?> c = clase; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(nombre);
            } catch (NoSuchFieldException ignorado) { /* sigue en la superclase */ }
        }
        return null;
    }

    /** Convierte el valor recibido (texto o numero del JSON) al tipo que espera la clase. */
    private static Object convertir(Object valor, Class<?> tipo, String propiedad) {
        if (valor == null) return null;
        try {
            String s = valor.toString().trim();
            if (tipo == double.class || tipo == Double.class) return Double.valueOf(s);
            if (tipo == float.class || tipo == Float.class) return Float.valueOf(s);
            if (tipo == int.class || tipo == Integer.class) return (int) Double.parseDouble(s);
            if (tipo == long.class || tipo == Long.class) return (long) Double.parseDouble(s);
            if (tipo == java.math.BigDecimal.class) return new java.math.BigDecimal(s);
            if (tipo == boolean.class || tipo == Boolean.class) return Boolean.valueOf(s);
            if (tipo == String.class) return s;
            if (tipo == Object.class || tipo.isInstance(valor)) return valor;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El parametro '" + propiedad + "' debe ser numerico (recibido: " + valor + ")");
        }
        throw new IllegalArgumentException("Tipo no soportado para '" + propiedad + "': " + tipo.getSimpleName());
    }

    private static double redondear(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
