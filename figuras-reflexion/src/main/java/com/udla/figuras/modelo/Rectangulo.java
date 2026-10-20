package com.udla.figuras.modelo;

public class Rectangulo implements Figura {
    private double base;
    private double altura;

    public Rectangulo() { }

    public double getBase() { return base; }
    public void setBase(double base) { this.base = base; }
    public double getAltura() { return altura; }
    public void setAltura(double altura) { this.altura = altura; }

    @Override public double area() { return base * altura; }
    @Override public double perimetro() { return 2 * (base + altura); }
}
