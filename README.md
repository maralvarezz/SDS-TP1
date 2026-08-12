# SDS TP1 — Cell Index Method

## Correr la simulación

**Desde IntelliJ (mas simple, no necesita nada instalado aparte):** click derecho sobre `Main.java` (`src/main/java/ar/edu/itba/sds/Main.java`) → *Run 'Main.main()'*. Los argumentos (`--n=300 --rc=1.5 ...`) se ponen en *Run/Debug Configurations → Program arguments*.

**Desde terminal:**

```bash
mvn package
java -jar target/sds-tp1-1.0-SNAPSHOT.jar
```

Si la terminal dice `mvn: command not found`, es que no tenés Maven instalado a nivel sistema (IntelliJ trae el suyo propio, por eso desde ahí funciona sin instalar nada). Para tenerlo también en la terminal: `brew install maven` (con [Homebrew](https://brew.sh)).

Parámetros por `application.properties` o por línea de comandos (`--clave=valor`):

| Parámetro CLI | Descripción | Default |
|---|---|---|
| `--n` | Cantidad de partículas | 100 |
| `--l` | Lado del área | 20.0 |
| `--m` | Cantidad de celdas por lado | 10 |
| `--rc` | Radio de interacción | 1.0 |
| `--radius-min` / `--radius-max` | Rango de radios `ri` | 0.23 / 0.26 |
| `--periodic` | Condiciones periódicas de contorno | false |
| `--random-seed` | Semilla (vacío = aleatoria real) | — |
| `--input-mode` | `auto` (usa archivo si existe, si no genera), `random` (siempre genera), `file` (exige archivo) | auto |
| `--target` | Partícula a destacar en la figura / vista inicial del visualizador interactivo | 1 |

Ejemplo:

```bash
java -jar target/sds-tp1-1.0-SNAPSHOT.jar --n=300 --m=6 --rc=1.5 --periodic=true --target=42
```

## Gráficos (se generan solos en cada corrida)

Cada corrida escribe, además de `output/neighbours.txt`:

- `output/render_data.json`: posiciones, radios, `L`, `rc`, condición de contorno y el mapa de vecinos ya calculado — lo consumen los dos scripts de `viz/`.
- `output/figures/particles_<timestamp>.png` y `output/figures/latest.png`: la figura pedida en el punto 1 del enunciado (todas las partículas, la partícula pasada como `--target` de un color y sus vecinos de otro).

Esto lo dispara automáticamente `Main` llamando a `viz/plot_static.py` por `ProcessBuilder`. Si no hay Python/matplotlib instalados, la simulación igual termina bien (solo se avisa por stderr y se omite la figura).

Instalar dependencias de Python una vez:

```bash
pip3 install -r viz/requirements.txt
```

Para desactivar la generación automática: `--viz-enabled=false`.

## Visualización dinámica (demo en vivo)

```bash
python3 viz/interactive_viewer.py output/render_data.json
```

Abre una ventana con todas las partículas. Al hacer click sobre cualquiera, se ilumina esa partícula, se le dibuja un círculo punteado de radio `rc` (más su propio radio) y se resaltan todos sus vecinos reales (los que ya calculó el CIM en Java, respetando distancia borde-a-borde y condición de contorno). Con `periodic=true`, si el círculo se sale del área se dibujan también las copias "fantasma" en el borde opuesto para visualizar el wrap-around.

Sirve para la demostración en vivo del punto 2 del enunciado: se puede correr la simulación con distintos `N`, `L`, `M` y `rc`, y despues explorar vecinos a mano sobre esa corrida.
