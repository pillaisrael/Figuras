package com.udla.figuras.modelo;

/** Triangulo definido por sus tres lados (area con la formula de Heron). */
public class Triangulo implements Figura {
    private double ladoA;
    private double ladoB;
    private double ladoC;

    public Triangulo() { }

    public double getLadoA() { return ladoA; }
    public void setLadoA(double ladoA) { this.ladoA = ladoA; }
    public double getLadoB() { return ladoB; }
    public void setLadoB(double ladoB) { this.ladoB = ladoB; }
    public double getLadoC() { return ladoC; }
    public void setLadoC(double ladoC) { this.ladoC = ladoC; }

    @Override
    public double area() {
        double s = perimetro() / 2;
        return Math.sqrt(s * (s - ladoA) * (s - ladoB) * (s - ladoC));
    }

    @Override public double perimetro() { return ladoA + ladoB + ladoC; }
}
