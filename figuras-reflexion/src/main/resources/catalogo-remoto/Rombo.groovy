// Rombo definido por sus dos diagonales
import com.udla.figuras.modelo.Figura

class Rombo implements Figura {
    double diagonalMayor
    double diagonalMenor

    double area() { diagonalMayor * diagonalMenor / 2 }

    double perimetro() {
        4 * Math.sqrt(Math.pow(diagonalMayor / 2, 2) + Math.pow(diagonalMenor / 2, 2))
    }
}
