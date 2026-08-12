package ar.edu.itba.sds.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TimeWriter {
    private TimeWriter() {
    }

    public static void write(Path path, long elapsedNs) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        Files.writeString(path, Long.toString(elapsedNs));
    }
}
