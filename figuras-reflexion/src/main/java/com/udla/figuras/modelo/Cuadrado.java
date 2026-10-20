package com.udla.figuras.modelo;

public class Cuadrado implements Figura {
    private double lado;

    public Cuadrado() { }

    public double getLado() { return lado; }
    public void setLado(double lado) { this.lado = lado; }

    @Override public double area() { return lado * lado; }
    @Override public double perimetro() { return 4 * lado; }
}
