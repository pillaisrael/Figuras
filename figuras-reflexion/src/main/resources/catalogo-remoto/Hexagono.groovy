// Hexagono regular definido por su lado
import com.udla.figuras.modelo.Figura

class Hexagono implements Figura {
    double lado

    double area()      { (3 * Math.sqrt(3) / 2) * lado * lado }
    double perimetro() { 6 * lado }
}
