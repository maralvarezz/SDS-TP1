package ar.edu.itba.sds.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigValidatorTest {

    private SimulationConfig configWithTarget(int n, int targetParticleId) {
        return new SimulationConfig(
                n, 20.0, 10, 1.0, 0.23, 0.26, false, OptionalLong.empty(), "auto",
                Path.of("input/static.txt"), Path.of("input/dynamic.txt"), Path.of("output/neighbours.txt"),
                targetParticleId, true, "python3", Path.of("viz/plot_static.py"),
                Path.of("output/figures"), Path.of("output/render_data.json")
        );
    }

    @Test
    void acceptsTargetWithinRange() {
        assertDoesNotThrow(() -> ConfigValidator.validateTarget(configWithTarget(100, 1)));
        assertDoesNotThrow(() -> ConfigValidator.validateTarget(configWithTarget(100, 100)));
    }

    @Test
    void rejectsTargetBelowOne() {
        assertThrows(IllegalArgumentException.class, () -> ConfigValidator.validateTarget(configWithTarget(100, 0)));
    }

    @Test
    void rejectsTargetAboveN() {
        assertThrows(IllegalArgumentException.class, () -> ConfigValidator.validateTarget(configWithTarget(100, 101)));
    }
}
