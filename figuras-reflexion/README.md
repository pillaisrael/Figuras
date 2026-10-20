# Figuras por Reflexión — ISWZ2202

Aplicación Spring Boot que crea figuras geométricas **a partir del nombre de la clase y sus parámetros**, usando reflexión, y que permite **cargar figuras nuevas escritas en Groovy en tiempo de ejecución**.

## Requisitos
- Java 17 o superior
- Maven 3.9+ (o el Maven que trae IntelliJ / VS Code)

## Ejecutar
```bash
mvn spring-boot:run
```
Abre **http://localhost:8080**: hay una página para probar todo sin Postman.

Para correr las pruebas: `mvn test`

## Estructura
```
modelo/      Figura (interfaz) + figuras básicas: Circulo, Cuadrado, Rectangulo, Triangulo
servicio/
  RegistroFiguras        catálogo nombre -> Class (nativas descubiertas escaneando el paquete + Groovy)
  FabricaFiguras         REFLEXIÓN: instancia, asigna parámetros, invoca area()/perimetro(), inspecciona la clase
  CargadorGroovy         compila código Groovy con GroovyClassLoader y lo registra
  ServicioFigurasExterno EXTRA: consume un servicio HTTP que publica figuras nuevas
controlador/
  FiguraController       API REST
  CatalogoRemotoController  servicio externo SIMULADO (para que la demo funcione sin internet)
ejemplos-groovy/Pentagono.groovy   ejemplo para cargar a mano
figuras-dinamicas/                 aquí se guardan las figuras Groovy cargadas (se recompilan al iniciar)
```

## Cómo funciona la reflexión (paso a paso)
Para `("Circulo", radio=5)`:
1. `RegistroFiguras` traduce el nombre a un objeto `Class<?>` (las nativas se encontraron con `Class.forName`).
2. `clase.getDeclaredConstructor().newInstance()` crea el objeto **sin escribir `new Circulo()`**.
3. Por cada parámetro busca el setter `setRadio` con `getMethods()` y lo llama con `Method.invoke`; si no hay setter, escribe el atributo con `Field.set`. Los valores se convierten al tipo que espera la clase (texto → `double`, etc.).
4. `getMethod("area").invoke(obj)` y `getMethod("perimetro").invoke(obj)` calculan los resultados.

Como todo se hace por reflexión, una clase compilada en Groovy se usa **exactamente igual** que una nativa. Incluso `Elipse` (del catálogo remoto) **no implementa** `Figura` y funciona igual, porque solo se exige que tenga `area()` y `perimetro()` (duck typing).

## API
| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/api/figuras` | Lista figuras disponibles y sus parámetros |
| POST | `/api/figuras/crear` | Body: `{"figura":"Circulo","parametros":{"radio":5}}` |
| GET | `/api/figuras/Circulo?radio=5` | Lo mismo, rápido desde el navegador |
| GET | `/api/figuras/Circulo/info` | Inspección en tiempo de ejecución: atributos, métodos, constructores, classloader |
| POST | `/api/figuras/groovy` | Body (texto plano): código de una clase Groovy |
| GET | `/api/figuras/externas` | EXTRA: ver qué ofrece el servicio externo |
| POST | `/api/figuras/externas/importar` | EXTRA: descargar y compilar todas las figuras del servicio |

Ejemplos con curl:
```bash
curl -X POST localhost:8080/api/figuras/crear -H "Content-Type: application/json" \
     -d '{"figura":"Circulo","parametros":{"radio":5}}'

curl -X POST localhost:8080/api/figuras/groovy -H "Content-Type: text/plain" \
     --data-binary @ejemplos-groovy/Pentagono.groovy

curl "localhost:8080/api/figuras/Pentagono?lado=5"

curl -X POST localhost:8080/api/figuras/externas/importar
curl "localhost:8080/api/figuras/Hexagono?lado=2"
```

## Escribir una figura en Groovy
Reglas: debe ser una **clase** con constructor vacío y métodos públicos `area()` y `perimetro()` que devuelvan un número. Cada atributo es un parámetro.
```groovy
import com.udla.figuras.modelo.Figura

class Pentagono implements Figura {
    double lado
    double area()      { 0.25 * Math.sqrt(5 * (5 + 2 * Math.sqrt(5))) * lado * lado }
    double perimetro() { 5 * lado }
}
```

## Servicio externo (extra)
`ServicioFigurasExterno` hace un GET con `RestClient` a la URL de `figuras.servicio-externo.url` y espera un JSON:
```json
[ { "nombre": "Hexagono", "descripcion": "...", "codigo": "class Hexagono { ... }" } ]
```
Por defecto apunta a `/api/catalogo-remoto/figuras` (servicio simulado de la propia app, con Hexagono, Trapecio, Rombo y Elipse). Se puede usar otra URL cambiando `application.properties` o con `?url=...`.

## Nota de seguridad
Compilar y ejecutar código recibido por la red es peligroso: quien envíe el código puede ejecutar cualquier cosa en el servidor. En un sistema real habría que autenticar el endpoint y restringir el código (por ejemplo con `SecureASTCustomizer` de Groovy o un sandbox).
