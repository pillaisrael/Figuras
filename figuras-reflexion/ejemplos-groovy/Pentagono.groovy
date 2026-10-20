// Pentagono regular definido por la longitud de su lado
import com.udla.figuras.modelo.Figura

class Pentagono implements Figura {
    double lado

    double area() {
        // A = (1/4) * sqrt(5 * (5 + 2*sqrt(5))) * lado^2
        return 0.25 * Math.sqrt(5 * (5 + 2 * Math.sqrt(5))) * lado * lado
    }

    double perimetro() {
        return 5 * lado
    }
}
