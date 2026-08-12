package ar.edu.itba.sds.io;

import ar.edu.itba.sds.model.StaticParticle;
import ar.edu.itba.sds.model.StaticSystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class StaticFileReader {
    private StaticFileReader() {
    }

    public static StaticSystem read(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();

        if (lines.size() < 2) {
            throw new IllegalArgumentException("El archivo estatico debe tener al menos N y L");
        }

        int n = Integer.parseInt(lines.get(0));
        double l = Double.parseDouble(lines.get(1));
        if (lines.size() != n + 2) {
            throw new IllegalArgumentException("El archivo estatico declara N=" + n + " pero contiene " + (lines.size() - 2) + " particulas");
        }

        List<StaticParticle> particles = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            String[] parts = lines.get(i + 2).split("\\s+");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Linea estatica invalida para particula " + (i + 1));
            }
            particles.add(new StaticParticle(i + 1, Double.parseDouble(parts[0]), Double.parseDouble(parts[1])));
        }

        return new StaticSystem(n, l, List.copyOf(particles));
    }
}
