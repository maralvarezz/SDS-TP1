package ar.edu.itba.sds.config;

import java.nio.file.Path;
import java.util.OptionalLong;

public record SimulationConfig(
        int n,
        double l,
        int m,
        double rc,
        double radiusMin,
        double radiusMax,
        boolean periodic,
        OptionalLong randomSeed,
        String inputMode,
        Path staticFile,
        Path dynamicFile,
        Path neighboursFile,
        Path timeFile,
        int targetParticleId,
        boolean vizEnabled,
        String vizPythonExecutable,
        Path vizPlotScript,
        Path vizOutputDir,
        Path vizRenderDataFile
) {
}
