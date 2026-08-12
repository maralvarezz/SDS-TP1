package ar.edu.itba.sds.io;

import ar.edu.itba.sds.model.StaticParticle;
import ar.edu.itba.sds.model.StaticSystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class StaticFileWriter {
    private StaticFileWriter() {
    }

    public static void write(Path path, StaticSystem staticSystem) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        List<String> lines = new ArrayList<>();
        lines.add(Integer.toString(staticSystem.n()));
        lines.add(Double.toString(staticSystem.l()));
        for (StaticParticle particle : staticSystem.particles()) {
            lines.add(particle.radius() + " " + particle.property());
        }
        Files.write(path, lines);
    }
}
