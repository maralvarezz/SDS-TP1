package ar.edu.itba.sds.io;

import ar.edu.itba.sds.model.Particle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderDataWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void writesValidJsonStructure() throws IOException {
        List<Particle> particles = List.of(
                new Particle(1, 1.0, 2.0, 0.25, 0),
                new Particle(2, 3.0, 4.0, 0.25, 0)
        );
        Map<Integer, Set<Integer>> neighbours = new HashMap<>();
        neighbours.put(1, Set.of(2));
        neighbours.put(2, Set.of(1));

        Path output = tempDir.resolve("render_data.json");
        RenderDataWriter.write(output, 20.0, 1.0, false, 1, particles, neighbours);

        String content = Files.readString(output);
        assertTrue(content.contains("\"l\": 20.0"));
        assertTrue(content.contains("\"rc\": 1.0"));
        assertTrue(content.contains("\"periodic\": false"));
        assertTrue(content.contains("\"targetId\": 1"));
        assertTrue(content.contains("\"id\": 1, \"x\": 1.0, \"y\": 2.0, \"radius\": 0.25"));
        assertTrue(content.contains("\"1\": [2]"));
        assertTrue(content.contains("\"2\": [1]"));

        // valida que python pueda parsearlo (mismo formato que consumen los scripts de viz)
        ProcessBuilder pb = new ProcessBuilder("python3", "-c",
                "import json,sys; json.load(open(sys.argv[1]))", output.toString());
        pb.inheritIO();
        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            return; // python3 no disponible en este entorno, se omite la validacion cruzada
        }
        try {
            int exit = process.waitFor();
            assertTrue(exit == 0, "El JSON generado no es valido para Python");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
