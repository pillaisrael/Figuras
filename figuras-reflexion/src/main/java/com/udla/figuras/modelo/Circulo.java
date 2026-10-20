package com.udla.figuras.modelo;

public class Circulo implements Figura {
    private double radio;

    public Circulo() { }

    public double getRadio() { return radio; }
    public void setRadio(double radio) { this.radio = radio; }

    @Override public double area() { return Math.PI * radio * radio; }
    @Override public double perimetro() { return 2 * Math.PI * radio; }
}
