package ar.edu.itba.sds;

import ar.edu.itba.sds.algorithm.CellIndexMethod;
import ar.edu.itba.sds.config.ConfigLoader;
import ar.edu.itba.sds.config.ConfigValidator;
import ar.edu.itba.sds.config.SimulationConfig;
import ar.edu.itba.sds.generator.ParticleGenerator;
import ar.edu.itba.sds.io.DynamicFileReader;
import ar.edu.itba.sds.io.DynamicFileWriter;
import ar.edu.itba.sds.io.NeighbourWriter;
import ar.edu.itba.sds.io.StaticFileReader;
import ar.edu.itba.sds.io.StaticFileWriter;
import ar.edu.itba.sds.model.Particle;
import ar.edu.itba.sds.model.StaticSystem;
import ar.edu.itba.sds.validation.ParticleSystemValidator;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
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

        StaticSystem staticSystem = obtainStaticSystem(config);
        ParticleSystemValidator.validateStaticSystem(staticSystem, config);
        ConfigValidator.validateGeometry(config, staticSystem.maxRadius());

        List<Particle> particles = obtainDynamicParticles(config, staticSystem);
        ParticleSystemValidator.validateDynamicParticles(particles, staticSystem);

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
        System.out.println("Busqueda CIM completada en " + elapsed + " ns");
        System.out.println("Vecinos escritos en " + config.neighboursFile());
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
