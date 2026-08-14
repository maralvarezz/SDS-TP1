package ar.edu.itba.sds.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NeighbourWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void writesParticleIdFollowedBySortedNeighbourIds() throws IOException {
        Path output = tempDir.resolve("neighbours.txt");
        Map<Integer, Set<Integer>> neighbours = Map.of(
                1, Set.of(3, 2),
                2, Set.of(1),
                3, Set.of()
        );

        NeighbourWriter.write(output, 3, neighbours);

        assertEquals(List.of("1   2 3", "2   1", "3"), Files.readAllLines(output));
    }
}
