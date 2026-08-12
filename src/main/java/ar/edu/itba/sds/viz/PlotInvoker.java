package ar.edu.itba.sds.viz;

import ar.edu.itba.sds.config.SimulationConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Dispara la generacion automatica de la figura estatica (punto 1 del enunciado:
 * posiciones de todas las particulas, la particula objetivo de un color y sus
 * vecinos de otro) invocando el script de Python via ProcessBuilder. Si Python
 * o matplotlib no estan disponibles, no rompe la corrida: avisa por stderr y
 * el resto de la simulacion (vecinos + tiempo) queda igual generado.
 */
public final class PlotInvoker {
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private PlotInvoker() {
    }

    public static void generateStaticFigure(SimulationConfig config) {
        if (!config.vizEnabled()) {
            return;
        }
        if (!Files.exists(config.vizPlotScript())) {
            System.err.println("Aviso: no se encontro el script de graficos en " + config.vizPlotScript() + ", se omite la figura.");
            return;
        }

        try {
            Files.createDirectories(config.vizOutputDir());
        } catch (IOException e) {
            System.err.println("Aviso: no se pudo crear el directorio de figuras " + config.vizOutputDir() + ": " + e.getMessage());
            return;
        }

        String timestamp = LocalDateTime.now().format(TIMESTAMP);
        Path timestampedFigure = config.vizOutputDir().resolve("particles_" + timestamp + ".png");
        Path latestFigure = config.vizOutputDir().resolve("latest.png");

        boolean ok = runPlotScript(config, timestampedFigure);
        if (ok) {
            copyToLatest(timestampedFigure, latestFigure);
            System.out.println("Figura generada en " + timestampedFigure + " (y " + latestFigure + ")");
        }
    }

    private static boolean runPlotScript(SimulationConfig config, Path outputFigure) {
        ProcessBuilder builder = new ProcessBuilder(
                config.vizPythonExecutable(),
                config.vizPlotScript().toString(),
                config.vizRenderDataFile().toString(),
                outputFigure.toString()
        );
        builder.redirectErrorStream(false);
        builder.inheritIO();

        try {
            Process process = builder.start();
            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                System.err.println("Aviso: la generacion de la figura tardo demasiado y se cancelo.");
                return false;
            }
            if (process.exitValue() != 0) {
                System.err.println("Aviso: el script de graficos termino con codigo " + process.exitValue() + ", se omite la figura.");
                return false;
            }
            return true;
        } catch (IOException e) {
            System.err.println("Aviso: no se pudo ejecutar " + config.vizPythonExecutable()
                    + " (¿esta instalado y en el PATH?). Se omite la figura. Detalle: " + e.getMessage());
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Aviso: la generacion de la figura fue interrumpida.");
            return false;
        }
    }

    private static void copyToLatest(Path source, Path destination) {
        try {
            Files.copy(source, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Aviso: no se pudo actualizar " + destination + ": " + e.getMessage());
        }
    }
}
