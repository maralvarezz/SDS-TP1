package ar.edu.itba.sds.config;

public final class ConfigValidator {
    private ConfigValidator() {
    }

    public static void validateBasic(SimulationConfig config) {
        if (config.n() <= 0) {
            throw new IllegalArgumentException("N debe ser mayor a 0");
        }
        if (config.l() <= 0) {
            throw new IllegalArgumentException("L debe ser mayor a 0");
        }
        if (config.rc() <= 0) {
            throw new IllegalArgumentException("rc debe ser mayor a 0");
        }
        if (config.m() < 3) {
            throw new IllegalArgumentException("M debe ser mayor o igual a 3");
        }
        if (config.radiusMin() <= 0 || config.radiusMax() <= 0) {
            throw new IllegalArgumentException("Los radios minimo y maximo deben ser mayores a 0");
        }
        if (config.radiusMin() > config.radiusMax()) {
            throw new IllegalArgumentException("radiusMin debe ser menor o igual a radiusMax");
        }
        if (!"auto".equalsIgnoreCase(config.inputMode())
                && !"random".equalsIgnoreCase(config.inputMode())
                && !"file".equalsIgnoreCase(config.inputMode())) {
            throw new IllegalArgumentException("input-mode debe ser auto, random o file");
        }
    }

    public static void validateGeometry(SimulationConfig config, double rMax) {
        double cellLength = config.l() / config.m();
        double minimumCellLength = config.rc() + 2 * rMax;
        int maxM = (int) Math.floor(config.l() / minimumCellLength);

        if (config.l() < 3 * minimumCellLength || config.m() > maxM) {
            throw new IllegalArgumentException("""
                    Configuracion geometrica invalida para CIM:
                    M recibido: %d
                    L recibido: %.6f
                    cellLength: %.6f
                    cellLength minimo requerido: %.6f
                    M maximo permitido: %d
                    """.formatted(config.m(), config.l(), cellLength, minimumCellLength, maxM));
        }
    }
}
