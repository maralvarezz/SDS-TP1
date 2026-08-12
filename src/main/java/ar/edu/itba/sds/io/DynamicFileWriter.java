package ar.edu.itba.sds.io;

import ar.edu.itba.sds.model.Particle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class DynamicFileWriter {
    private DynamicFileWriter() {
    }

    public static void write(Path path, List<Particle> particles) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        List<String> lines = new ArrayList<>();
        lines.add("0");
        for (Particle particle : particles) {
            lines.add(particle.x() + " " + particle.y());
        }
        Files.write(path, lines);
    }
}
