package ar.edu.itba.sds.io;

import ar.edu.itba.sds.model.Particle;
import ar.edu.itba.sds.model.StaticParticle;
import ar.edu.itba.sds.model.StaticSystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class DynamicFileReader {
    private DynamicFileReader() {
    }

    public static List<Particle> read(Path path, StaticSystem staticSystem) throws IOException {
        List<String> lines = Files.readAllLines(path).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();

        if (lines.size() != staticSystem.n() + 1) {
            throw new IllegalArgumentException("El archivo dinamico debe tener t0 y " + staticSystem.n() + " posiciones");
        }

        List<Particle> particles = new ArrayList<>(staticSystem.n());
        for (int i = 0; i < staticSystem.n(); i++) {
            String[] parts = lines.get(i + 1).split("\\s+");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Linea dinamica invalida para particula " + (i + 1));
            }
            StaticParticle staticParticle = staticSystem.particles().get(i);
            particles.add(new Particle(
                    staticParticle.id(),
                    Double.parseDouble(parts[0]),
                    Double.parseDouble(parts[1]),
                    staticParticle.radius(),
                    staticParticle.property()
            ));
        }
        return List.copyOf(particles);
    }
}
