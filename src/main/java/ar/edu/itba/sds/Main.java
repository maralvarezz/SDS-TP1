package ar.edu.itba.sds;

import ar.edu.itba.sds.algorithm.CellIndexMethod;
import ar.edu.itba.sds.config.ConfigLoader;
import ar.edu.itba.sds.config.ConfigValidator;
import ar.edu.itba.sds.config.SimulationConfig;
import ar.edu.itba.sds.generator.ParticleGenerator;
import ar.edu.itba.sds.io.DynamicFileReader;
import ar.edu.itba.sds.io.DynamicFileWriter;
import ar.edu.itba.sds.io.NeighbourWriter;
import ar.edu.itba.sds.io.RenderDataWriter;
import ar.edu.itba.sds.io.StaticFileReader;
import ar.edu.itba.sds.io.StaticFileWriter;
import ar.edu.itba.sds.io.TimeWriter;
import ar.edu.itba.sds.model.Particle;
import ar.edu.itba.sds.model.StaticSystem;
import ar.edu.itba.sds.validation.ParticleSystemValidator;
import ar.edu.itba.sds.viz.PlotInvoker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        try {
            run(args);
        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void run(String[] args) throws IOException {
        SimulationConfig config = ConfigLoader.load(args);
        ConfigValidator.validateBasic(config);
        ConfigValidator.validateWarmup(config);

        if (config.experimentEnabled()) {
            ConfigValidator.validateExperiment(config);
            runExperiment(config);
            return;
        }

        ConfigValidator.validateTarget(config);
        runSingle(config);
    }

    private static void runSingle(SimulationConfig config) throws IOException {
        StaticSystem staticSystem = obtainStaticSystem(config);
        ParticleSystemValidator.validateStaticSystem(staticSystem, config);
        ConfigValidator.validateGeometry(config, staticSystem.maxRadius());

        List<Particle> particles = obtainDynamicParticles(config, staticSystem);
        ParticleSystemValidator.validateDynamicParticles(particles, staticSystem);

        warmUpVm(config);

        long start = System.nanoTime();
        Map<Integer, Set<Integer>> neighbours = CellIndexMethod.findNeighbours(
                particles,
                staticSystem.l(),
                config.m(),
                config.rc(),
                config.periodic()
        );
        long elapsed = System.nanoTime() - start;

        NeighbourWriter.write(config.neighboursFile(), staticSystem.n(), neighbours);
        TimeWriter.write(config.timeFile(), elapsed);
        System.out.println("Busqueda CIM completada en " + elapsed + " ns");
        System.out.println("Vecinos escritos en " + config.neighboursFile());
        System.out.println("Tiempo escrito en " + config.timeFile());

        if (config.vizEnabled()) {
            RenderDataWriter.write(
                    config.vizRenderDataFile(),
                    staticSystem.l(),
                    config.rc(),
                    config.periodic(),
                    config.targetParticleId(),
                    particles,
                    neighbours
            );

            // Genera la figura pedida solo si se habilita por parametro/config.
            //PlotInvoker.generateStaticFigure(config);
        }
    }

    private static void runExperiment(SimulationConfig config) throws IOException {
        warmUpVm(config);

        List<String> runLines = new ArrayList<>();
        runLines.add("variable,value,run,n,m,l,density,elapsed_ns,elapsed_ms");
        Map<Integer, List<Long>> elapsedByValue = new LinkedHashMap<>();

        List<Integer> values = config.experimentValues();
        for (int valueIndex = 0; valueIndex < values.size(); valueIndex++) {
            int value = values.get(valueIndex);
            for (int run = 1; run <= config.experimentRunsPerValue(); run++) {
                SimulationConfig runConfig = configForExperimentRun(config, value, valueIndex, run);
                ConfigValidator.validateBasic(runConfig);

                StaticSystem staticSystem = ParticleGenerator.generateStaticSystem(runConfig);
                ParticleSystemValidator.validateStaticSystem(staticSystem, runConfig);
                ConfigValidator.validateGeometry(runConfig, staticSystem.maxRadius());
                List<Particle> particles = ParticleGenerator.generateDynamicParticles(staticSystem, runConfig);
                ParticleSystemValidator.validateDynamicParticles(particles, staticSystem);

                long start = System.nanoTime();
                CellIndexMethod.findNeighbours(
                        particles,
                        staticSystem.l(),
                        runConfig.m(),
                        runConfig.rc(),
                        runConfig.periodic()
                );
                long elapsed = System.nanoTime() - start;

                elapsedByValue.computeIfAbsent(value, ignored -> new ArrayList<>()).add(elapsed);
                runLines.add(String.format(
                        Locale.US,
                        "%s,%d,%d,%d,%d,%.12g,%.12g,%d,%.6f",
                        runConfig.experimentVariable().toUpperCase(Locale.ROOT),
                        value,
                        run,
                        runConfig.n(),
                        runConfig.m(),
                        staticSystem.l(),
                        runConfig.n() / (staticSystem.l() * staticSystem.l()),
                        elapsed,
                        elapsed / 1_000_000.0
                ));
                System.out.printf(
                        Locale.US,
                        "Experimento %s=%d corrida %d/%d: %d ns%n",
                        runConfig.experimentVariable().toUpperCase(Locale.ROOT),
                        value,
                        run,
                        runConfig.experimentRunsPerValue(),
                        elapsed
                );
            }
        }

        writeLines(config.experimentRunsFile(), runLines);
        writeLines(config.experimentSummaryFile(), summaryLines(elapsedByValue));
        System.out.println("Corridas escritas en " + config.experimentRunsFile());
        System.out.println("Resumen escrito en " + config.experimentSummaryFile());
    }

    private static void warmUpVm(SimulationConfig config) {
        if (!config.warmupEnabled() || config.warmupIterations() == 0) {
            return;
        }

        SimulationConfig warmupConfig = copyConfig(
                config,
                config.warmupN(),
                config.l(),
                config.m(),
                OptionalLong.of(config.randomSeed().orElse(0L)),
                "random"
        );
        StaticSystem staticSystem = ParticleGenerator.generateStaticSystem(warmupConfig);
        List<Particle> particles = ParticleGenerator.generateDynamicParticles(staticSystem, warmupConfig);

        for (int i = 0; i < config.warmupIterations(); i++) {
            CellIndexMethod.findNeighbours(
                    particles,
                    staticSystem.l(),
                    warmupConfig.m(),
                    warmupConfig.rc(),
                    warmupConfig.periodic()
            );
        }
        System.out.println("Warm-up completo (" + config.warmupIterations()
                + " llamadas con n=" + config.warmupN() + ")");
    }

    private static SimulationConfig configForExperimentRun(
            SimulationConfig config,
            int value,
            int valueIndex,
            int run
    ) {
        int n = "n".equalsIgnoreCase(config.experimentVariable()) ? value : config.n();
        int m = "m".equalsIgnoreCase(config.experimentVariable()) ? value : config.m();
        OptionalLong seed = config.randomSeed().isPresent()
                ? OptionalLong.of(config.randomSeed().getAsLong()
                + (long) valueIndex * config.experimentRunsPerValue()
                + run - 1)
                : OptionalLong.empty();
        return copyConfig(config, n, config.l(), m, seed, "random");
    }

    private static SimulationConfig copyConfig(
            SimulationConfig config,
            int n,
            double l,
            int m,
            OptionalLong randomSeed,
            String inputMode
    ) {
        return new SimulationConfig(
                n,
                l,
                m,
                config.rc(),
                config.radiusMin(),
                config.radiusMax(),
                config.periodic(),
                randomSeed,
                inputMode,
                config.staticFile(),
                config.dynamicFile(),
                config.neighboursFile(),
                config.timeFile(),
                Math.min(config.targetParticleId(), n),
                config.vizEnabled(),
                config.vizPythonExecutable(),
                config.vizPlotScript(),
                config.vizOutputDir(),
                config.vizRenderDataFile(),
                config.warmupEnabled(),
                config.warmupN(),
                config.warmupIterations(),
                config.experimentEnabled(),
                config.experimentVariable(),
                config.experimentValues(),
                config.experimentRunsPerValue(),
                config.experimentRunsFile(),
                config.experimentSummaryFile()
        );
    }

    private static List<String> summaryLines(Map<Integer, List<Long>> elapsedByValue) {
        List<String> lines = new ArrayList<>();
        lines.add("value,runs,mean_ms,stdev_ms,stderr_ms");

        for (Map.Entry<Integer, List<Long>> entry : elapsedByValue.entrySet()) {
            List<Long> samples = entry.getValue();
            double mean = samples.stream().mapToDouble(value -> value / 1_000_000.0).average().orElse(0);
            double variance = 0;
            if (samples.size() > 1) {
                for (long sample : samples) {
                    double elapsedMs = sample / 1_000_000.0;
                    variance += Math.pow(elapsedMs - mean, 2);
                }
                variance /= samples.size() - 1;
            }
            double stdev = Math.sqrt(variance);
            double stderr = samples.isEmpty() ? 0 : stdev / Math.sqrt(samples.size());
            lines.add(String.format(
                    Locale.US,
                    "%d,%d,%.6f,%.6f,%.6f",
                    entry.getKey(),
                    samples.size(),
                    mean,
                    stdev,
                    stderr
            ));
        }

        return lines;
    }

    private static void writeLines(Path path, List<String> lines) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        Files.write(path, lines);
    }

    private static StaticSystem obtainStaticSystem(SimulationConfig config) throws IOException {
        if ("random".equalsIgnoreCase(config.inputMode())) {
            StaticSystem staticSystem = ParticleGenerator.generateStaticSystem(config);
            StaticFileWriter.write(config.staticFile(), staticSystem);
            return staticSystem;
        }

        if ("file".equalsIgnoreCase(config.inputMode()) && !Files.exists(config.staticFile())) {
            throw new IllegalArgumentException("input-mode=file requiere archivo estatico existente: " + config.staticFile());
        }

        if (Files.exists(config.staticFile())) {
            return StaticFileReader.read(config.staticFile());
        }

        StaticSystem staticSystem = ParticleGenerator.generateStaticSystem(config);
        StaticFileWriter.write(config.staticFile(), staticSystem);
        return staticSystem;
    }

    private static List<Particle> obtainDynamicParticles(SimulationConfig config, StaticSystem staticSystem) throws IOException {
        if ("random".equalsIgnoreCase(config.inputMode())) {
            List<Particle> particles = ParticleGenerator.generateDynamicParticles(staticSystem, config);
            DynamicFileWriter.write(config.dynamicFile(), particles);
            return particles;
        }

        if ("file".equalsIgnoreCase(config.inputMode()) && !Files.exists(config.dynamicFile())) {
            throw new IllegalArgumentException("input-mode=file requiere archivo dinamico existente: " + config.dynamicFile());
        }

        if (Files.exists(config.dynamicFile())) {
            return DynamicFileReader.read(config.dynamicFile(), staticSystem);
        }

        List<Particle> particles = ParticleGenerator.generateDynamicParticles(staticSystem, config);
        DynamicFileWriter.write(config.dynamicFile(), particles);
        return particles;
    }
}
