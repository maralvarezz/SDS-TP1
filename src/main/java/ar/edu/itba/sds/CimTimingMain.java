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
