package ar.edu.itba.sds.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class NeighbourWriter {
    private NeighbourWriter() {
    }

    public static void write(Path path, int n, Map<Integer, Set<Integer>> neighbours) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        List<String> lines = new ArrayList<>(n);
        for (int id = 1; id <= n; id++) {
            String neighbourIds = neighbours.getOrDefault(id, Set.of()).stream()
                    .sorted()
                    .map(String::valueOf)
                    .collect(Collectors.joining(" "));
            lines.add(neighbourIds.isEmpty() ? String.valueOf(id) : id + "   " + neighbourIds);
        }
        Files.write(path, lines);
    }
}
