# SDS TP1 — Cell Index Method

## Correr la simulación

**Desde IntelliJ (mas simple, no necesita nada instalado aparte):** click derecho sobre `Main.java` (`src/main/java/ar/edu/itba/sds/Main.java`) → *Run 'Main.main()'*. Los argumentos (`--n=300 --rc=1.5 ...`) se ponen en *Run/Debug Configurations → Program arguments*.

**Desde terminal:**

```bash
mvn package
java -jar target/sds-tp1-1.0-SNAPSHOT.jar
```

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

## Graficos de particulas

Por defecto, cada corrida escribe `output/neighbours.txt` y `output/time.txt`.
Si se pide con `--viz-enabled=true`, tambien escribe:

- `output/render_data.json`: posiciones, radios, `L`, `rc`, condición de contorno y el mapa de vecinos ya calculado — lo consumen los dos scripts de `viz/`.
- `output/figures/particles_<timestamp>.png` y `output/figures/latest.png`: la figura pedida en el punto 1 del enunciado (todas las partículas, la partícula pasada como `--target` de un color y sus vecinos de otro).

Esto lo dispara `Main` llamando a `viz/plot_static.py` por `ProcessBuilder`. Si no hay Python/matplotlib instalados, la simulacion igual termina bien (solo se avisa por stderr y se omite la figura).

Instalar dependencias de Python una vez:

```bash
pip3 install -r viz/requirements.txt
```

Para generar la figura estatica y el JSON de render: `--viz-enabled=true`.

## Visualización dinámica (demo en vivo)

```bash
java -jar target/sds-tp1-1.0-SNAPSHOT.jar --viz-enabled=true
python3 viz/interactive_viewer.py output/render_data.json
```

Abre una ventana con todas las partículas. Al hacer click sobre cualquiera, se ilumina esa partícula, se le dibuja un círculo punteado de radio `rc` (más su propio radio) y se resaltan todos sus vecinos reales (los que ya calculó el CIM en Java, respetando distancia borde-a-borde y condición de contorno). Con `periodic=true`, si el círculo se sale del área se dibujan también las copias "fantasma" en el borde opuesto para visualizar el wrap-around.

Sirve para la demostración en vivo del punto 2 del enunciado: se puede correr la simulación con distintos `N`, `L`, `M` y `rc`, y despues explorar vecinos a mano sobre esa corrida.

## Analisis de tiempo variando N o M

El script `viz/time_analysis.py` corre varias ejecuciones del CIM y genera un
grafico de linea con marcadores y barras de error. Usa los tiempos que escribe
`Main` con `TimeWriter`; el script no mide el tiempo por su cuenta.

Cada analisis queda guardado en una carpeta propia dentro de `output/figures/`.
Por ejemplo, al variar `M` se crea:

```text
output/figures/time_M_<timestamp>/
  time_M_<timestamp>.png
  time_M_<timestamp>_runs.csv
  time_M_<timestamp>_summary.csv
  metadata.json
```

`metadata.json` guarda los parametros estaticos del analisis y
`*_runs.csv` guarda los datos que varian por corrida. Con esos archivos alcanza
para regenerar el grafico sin volver a correr las simulaciones. Los archivos
pesados/intermedios de cada corrida se siguen usando solo temporalmente y se
borran al final.

Para recrear el grafico variando `N` de 20 a 100 en intervalos de 10, dejando
fijos `M=10`, `L=20` y `rc=1`:

```bash
python viz/time_analysis.py --variable n --values 20 30 40 50 60 70 80 90 100 --runs-per-value 10 --m 10 --l 20 --rc 1 --compile
```

Salida:

```text
output/figures/time_N_<timestamp>/time_N_<timestamp>.png
```

Para recrear el grafico variando `M` de 1 a 10, dejando fijos `N=150`, `L=50`
y `rc=1`:

```bash
python viz/time_analysis.py --variable m --values 1 2 3 4 5 6 7 8 9 10 --runs-per-value 10 --n 150 --l 50 --rc 1 --compile
```

Salida:

```text
output/figures/time_M_<timestamp>/time_M_<timestamp>.png
```

Para regenerar un grafico ya guardado sin ejecutar Java:

```bash
python viz/time_analysis.py --replot-dir output/figures/time_M_<timestamp>
```

En el analisis variando `M`, la primera corrida crea un unico set de particulas
y todas las demas corridas usan esos mismos archivos con `--input-mode=file`.
En el analisis variando `N`, cada `N` genera su propio set de particulas con la
misma seed y las repeticiones de ese `N` reutilizan los archivos generados.
