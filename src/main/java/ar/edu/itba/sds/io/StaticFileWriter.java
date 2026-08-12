package ar.edu.itba.sds.io;

import ar.edu.itba.sds.model.StaticParticle;
import ar.edu.itba.sds.model.StaticSystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class StaticFileWriter {
    private StaticFileWriter() {
    }

    public static void write(Path path, StaticSystem staticSystem) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        List<String> lines = new ArrayList<>();
        lines.add(String.format(Locale.US, "%7d", staticSystem.n()));
        lines.add(formatBoxLength(staticSystem.l()));
        for (StaticParticle particle : staticSystem.particles()) {
            lines.add(String.format(Locale.US, "%10.4f%10.4f", particle.radius(), particle.property()));
        }
        Files.write(path, lines);
    }

    private static String formatBoxLength(double l) {
        if (l == Math.rint(l)) {
            return String.format(Locale.US, "%7.0f", l);
        }
        return String.format(Locale.US, "%10.4f", l);
    }
}
