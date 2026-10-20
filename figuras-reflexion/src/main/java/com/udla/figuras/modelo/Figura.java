package com.udla.figuras.modelo;

/**
 * Contrato comun de todas las figuras (nativas en Java o cargadas en Groovy).
 * La aplicacion NUNCA llama a estos metodos directamente: los descubre e
 * invoca por reflexion, de modo que tambien funcionaria con clases que solo
 * "se parecen" a una figura (duck typing).
 */
public interface Figura {
    double area();
    double perimetro();
}
