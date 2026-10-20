// Trapecio isosceles: baseMayor, baseMenor y altura
import com.udla.figuras.modelo.Figura

class Trapecio implements Figura {
    double baseMayor
    double baseMenor
    double altura

    double area() { (baseMayor + baseMenor) * altura / 2 }

    double perimetro() {
        double lado = Math.sqrt(altura * altura + Math.pow((baseMayor - baseMenor) / 2, 2))
        baseMayor + baseMenor + 2 * lado
    }
}
