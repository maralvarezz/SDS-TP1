package ar.edu.itba.sds.generator;

import ar.edu.itba.sds.config.SimulationConfig;
import ar.edu.itba.sds.distance.DistanceCalculator;
import ar.edu.itba.sds.model.Particle;
import ar.edu.itba.sds.model.StaticParticle;
import ar.edu.itba.sds.model.StaticSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class ParticleGenerator {
    private static final int MAX_ATTEMPTS_PER_PARTICLE = 100_000;

    private ParticleGenerator() {
    }

    public static StaticSystem generateStaticSystem(SimulationConfig config) {
        Random random = random(config);
        List<StaticParticle> particles = new ArrayList<>(config.n());
        for (int id = 1; id <= config.n(); id++) {
            double radius = config.radiusMin() == config.radiusMax()
                    ? config.radiusMin()
                    : config.radiusMin() + random.nextDouble(config.radiusMax() - config.radiusMin());
            particles.add(new StaticParticle(id, radius, 1));
        }
        return new StaticSystem(config.n(), config.l(), List.copyOf(particles));
    }

    public static List<Particle> generateDynamicParticles(StaticSystem staticSystem, SimulationConfig config) {
        Random random = random(config);
        List<Particle> particles = new ArrayList<>(staticSystem.n());

        for (StaticParticle staticParticle : staticSystem.particles()) {
            Particle candidate = null;
            for (int attempt = 0; attempt < MAX_ATTEMPTS_PER_PARTICLE; attempt++) {
                double bound = staticSystem.l() - 2 * staticParticle.radius();
                double x = staticParticle.radius() + random.nextDouble(bound);
                double y = staticParticle.radius() + random.nextDouble(bound);
                Particle next = new Particle(staticParticle.id(), x, y, staticParticle.radius(), staticParticle.property());
                if (!overlaps(next, particles, config.periodic(), staticSystem.l())) {
                    candidate = next;
                    break;
                }
            }
            if (candidate == null) {
                throw new IllegalStateException("No se pudo ubicar la particula " + staticParticle.id() + " sin superposicion");
            }
            particles.add(candidate);
        }

        return List.copyOf(particles);
    }

    private static boolean overlaps(Particle candidate, List<Particle> particles, boolean periodic, double l) {
        for (Particle particle : particles) {
            double distance = DistanceCalculator.centerDistance(candidate, particle, periodic, l);
            if (distance < candidate.radius() + particle.radius()) {
                return true;
            }
        }
        return false;
    }

    private static Random random(SimulationConfig config) {
        return config.randomSeed().isPresent() ? new Random(config.randomSeed().getAsLong()) : new Random();
    }
}
