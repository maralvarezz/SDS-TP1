package ar.edu.itba.sds.model;

import java.util.List;

public record StaticSystem(int n, double l, List<StaticParticle> particles) {
    public double maxRadius() {
        return particles.stream()
                .mapToDouble(StaticParticle::radius)
                .max()
                .orElseThrow(() -> new IllegalStateException("No hay particulas"));
    }
}
