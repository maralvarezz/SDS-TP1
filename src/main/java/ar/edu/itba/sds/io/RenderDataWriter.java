package ar.edu.itba.sds.io;

import ar.edu.itba.sds.model.Particle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Escribe un JSON autocontenido con todo lo necesario para graficar el sistema
 * (posiciones, radios, L, rc, condicion de borde, particula objetivo y el mapa
 * completo de vecinos ya calculado por el Cell Index Method). Tanto la figura
 * estatica que se genera en cada corrida como el visualizador interactivo leen
 * este mismo archivo, para no duplicar la logica de vecinos en Python.
 */
public final class RenderDataWriter {
    private RenderDataWriter() {
    }

    public static void write(
            Path path,
            double l,
            double rc,
            boolean periodic,
            int targetId,
            List<Particle> particles,
            Map<Integer, Set<Integer>> neighbours
    ) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"l\": ").append(l).append(",\n");
        json.append("  \"rc\": ").append(rc).append(",\n");
        json.append("  \"periodic\": ").append(periodic).append(",\n");
        json.append("  \"targetId\": ").append(targetId).append(",\n");

        json.append("  \"particles\": [\n");
        json.append(particles.stream()
                .map(p -> "    {\"id\": %d, \"x\": %s, \"y\": %s, \"radius\": %s}"
                        .formatted(p.id(), p.x(), p.y(), p.radius()))
                .collect(Collectors.joining(",\n")));
        json.append("\n  ],\n");

        json.append("  \"neighbours\": {\n");
        json.append(particles.stream()
                .map(p -> {
                    String ids = neighbours.getOrDefault(p.id(), Set.of()).stream()
                            .sorted()
                            .map(String::valueOf)
                            .collect(Collectors.joining(", "));
                    return "    \"%d\": [%s]".formatted(p.id(), ids);
                })
                .collect(Collectors.joining(",\n")));
        json.append("\n  }\n");
        json.append("}\n");

        Files.writeString(path, json.toString());
    }
}
