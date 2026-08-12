package ar.edu.itba.sds.validation;

import ar.edu.itba.sds.config.SimulationConfig;
import ar.edu.itba.sds.model.Particle;
import ar.edu.itba.sds.model.StaticParticle;
import ar.edu.itba.sds.model.StaticSystem;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ParticleSystemValidator {
    private ParticleSystemValidator() {
    }

    public static void validateStaticSystem(StaticSystem staticSystem, SimulationConfig config) {
        if (staticSystem.n() <= 0) {
            throw new IllegalArgumentException("El archivo estatico debe declarar N > 0");
        }
        if (staticSystem.l() <= 0) {
            throw new IllegalArgumentException("El archivo estatico debe declarar L > 0");
        }
        if (staticSystem.n() != staticSystem.particles().size()) {
            throw new IllegalArgumentException("El archivo estatico no coincide con N");
        }
        if (staticSystem.n() != config.n()) {
            throw new IllegalArgumentException("N de configuracion (" + config.n() + ") no coincide con el archivo estatico (" + staticSystem.n() + ")");
        }
        if (Double.compare(staticSystem.l(), config.l()) != 0) {
            throw new IllegalArgumentException("L de configuracion (" + config.l() + ") no coincide con el archivo estatico (" + staticSystem.l() + ")");
        }

        Set<Integer> ids = new HashSet<>();
        for (StaticParticle particle : staticSystem.particles()) {
            if (!ids.add(particle.id())) {
                throw new IllegalArgumentException("ID duplicado en archivo estatico: " + particle.id());
            }
            if (particle.radius() <= 0) {
                throw new IllegalArgumentException("Radio invalido para particula " + particle.id());
            }
        }
    }

    public static void validateDynamicParticles(List<Particle> particles, StaticSystem staticSystem) {
        if (particles.size() != staticSystem.n()) {
            throw new IllegalArgumentException("La cantidad de posiciones no coincide con N");
        }

        for (Particle particle : particles) {
            if (particle.x() < particle.radius() || particle.x() > staticSystem.l() - particle.radius()
                    || particle.y() < particle.radius() || particle.y() > staticSystem.l() - particle.radius()) {
                throw new IllegalArgumentException("La particula " + particle.id() + " esta fuera del dominio");
            }
        }
    }
}
