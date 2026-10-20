// Elipse (semiejes a y b). NO implementa Figura: funciona igual por reflexion (duck typing)
class Elipse {
    double a
    double b

    double area() { Math.PI * a * b }

    // Aproximacion de Ramanujan
    double perimetro() {
        Math.PI * (3 * (a + b) - Math.sqrt((3 * a + b) * (a + 3 * b)))
    }
}
