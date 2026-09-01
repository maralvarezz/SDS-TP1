package ar.edu.itba.sds;

import ar.edu.itba.sds.algorithm.CellIndexMethod;
import ar.edu.itba.sds.config.SimulationConfig;
import ar.edu.itba.sds.generator.ParticleGenerator;
import ar.edu.itba.sds.model.Particle;
import ar.edu.itba.sds.model.StaticSystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;

/**
 * Version TP1 del punto (g) del enunciado de TP2 (comparar tiempos del CIM entre TP1 y TP2), a
 * "las mismas condiciones" que la simulacion REAL de bandadas de TP2 (ver
 * ar.edu.itba.sds.tp2.experiment.CimTimingByModelMain): L=10 FIJO (el tamaño de caja que pide el
 * enunciado de TP2), rc=1, particulas PUNTUALES (radio 0, igual que las particulas de bandadas de
 * TP2 -- NO las particulas con radio 0.23-0.26 que usa TP1 para sus propios estudios), y
 * N=200,400,800 (las mismas rho=2,4,8 a L=10 que se estudiaron en los puntos a-f de TP2). Antes
 * esta clase escalaba L/M a densidad fija con particulas CON radio para llegar a N grandes
 * (10-5000) -- eso tenia sentido para comparar el CIM "en abstracto" en un rango amplio, pero no
 * es "las mismas condiciones" que las simulaciones reales de TP2, que es lo que pide el enunciado
 * y lo que valida la comparacion.
 * <p>
 * Un unico proceso Java, un solo warm-up al principio, mide los 3 N puertas adentro del mismo
 * proceso ya caliente -- evita el ruido de arranque/GC de lanzar una JVM nueva por N (ver
 * historial en viz/compare_cim_timing.py). Mismo formato de salida que antes: n,run,l,m,elapsed_ns.
 */
public final class CimTimingMain {

    private static final double L = 10.0;
    private static final int M = 10;
    private static final double RC = 1.0;
    private static final double RADIUS_MIN = 0.0;
    private static final double RADIUS_MAX = 0.0;
    private static final List<Integer> N_VALUES = List.of(200, 400, 800);
    private static final int RUNS_PER_VALUE = 10;
    private static final int WARMUP_ITERATIONS = 5000;

    private CimTimingMain() {
    }

    public static void main(String[] args) throws IOException {
        warmUp();

        List<String> lines = new ArrayList<>();
        lines.add("n,run,l,m,elapsed_ns");

        for (int n : N_VALUES) {
            for (int run = 0; run < RUNS_PER_VALUE; run++) {
                List<Particle> particles = randomParticles(n, run);

                long start = System.nanoTime();
                CellIndexMethod.findNeighbours(particles, L, M, RC, true);
                long elapsed = System.nanoTime() - start;

                lines.add(String.format(Locale.US, "%d,%d,%.4f,%d,%d", n, run, L, M, elapsed));
            }
            System.out.println("n=" + n + " listo (" + RUNS_PER_VALUE + " corridas)");
        }

        Path outputFile = Path.of("output/cim_timing_tp1.csv");
        if (outputFile.getParent() != null) {
            Files.createDirectories(outputFile.getParent());
        }
        Files.write(outputFile, lines);
        System.out.println("Tiempos escritos en " + outputFile.toAbsolutePath());
    }

    private static void warmUp() {
        List<Particle> particles = randomParticles(400, -1);

        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            CellIndexMethod.findNeighbours(particles, L, M, RC, true);
        }
        System.out.println("Warm-up completo (" + WARMUP_ITERATIONS + " llamadas)");
    }

    private static List<Particle> randomParticles(int n, int seedOffset) {
        SimulationConfig config = new SimulationConfig(
                n, L, M, RC, RADIUS_MIN, RADIUS_MAX, true, OptionalLong.of(1000L + seedOffset), "random",
                Path.of("unused"), Path.of("unused"), Path.of("unused"), Path.of("unused"),
                1, false, "python3", Path.of("unused"), Path.of("unused"), Path.of("unused"),
                true, 400, WARMUP_ITERATIONS,
                false, "n", List.of(), 1, Path.of("unused"), Path.of("unused")
        );
        StaticSystem staticSystem = ParticleGenerator.generateStaticSystem(config);
        return ParticleGenerator.generateDynamicParticles(staticSystem, config);
    }
}
