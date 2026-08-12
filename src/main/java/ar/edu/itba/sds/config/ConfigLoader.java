package ar.edu.itba.sds.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Properties;

public final class ConfigLoader {
    private static final String APPLICATION_PROPERTIES = "application.properties";

    private ConfigLoader() {
    }

    public static SimulationConfig load(String[] args) {
        Properties properties = internalDefaults();
        loadApplicationProperties(properties);
        applyCliArgs(properties, args);

        return new SimulationConfig(
                intProperty(properties, "simulation.n"),
                doubleProperty(properties, "simulation.l"),
                intProperty(properties, "simulation.m"),
                doubleProperty(properties, "simulation.rc"),
                doubleProperty(properties, "simulation.radius.min"),
                doubleProperty(properties, "simulation.radius.max"),
                booleanProperty(properties, "simulation.periodic"),
                optionalLongProperty(properties, "simulation.random-seed"),
                properties.getProperty("simulation.input-mode"),
                Path.of(properties.getProperty("simulation.input.static")),
                Path.of(properties.getProperty("simulation.input.dynamic")),
                Path.of(properties.getProperty("simulation.output.neighbours")),
                Path.of(properties.getProperty("simulation.output.time"))
        );
    }

    private static Properties internalDefaults() {
        Properties properties = new Properties();
        properties.setProperty("simulation.n", "100");
        properties.setProperty("simulation.l", "20.0");
        properties.setProperty("simulation.m", "10");
        properties.setProperty("simulation.rc", "1.0");
        properties.setProperty("simulation.radius.min", "0.23");
        properties.setProperty("simulation.radius.max", "0.26");
        properties.setProperty("simulation.periodic", "false");
        properties.setProperty("simulation.random-seed", "");
        properties.setProperty("simulation.input-mode", "auto");
        properties.setProperty("simulation.input.static", "input/static.txt");
        properties.setProperty("simulation.input.dynamic", "input/dynamic.txt");
        properties.setProperty("simulation.output.neighbours", "output/output.txt");
        properties.setProperty("simulation.output.time", "output/time.txt");
        return properties;
    }

    private static void loadApplicationProperties(Properties properties) {
        try (InputStream inputStream = ConfigLoader.class.getClassLoader().getResourceAsStream(APPLICATION_PROPERTIES)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo leer application.properties", e);
        }
    }

    private static void applyCliArgs(Properties properties, String[] args) {
        Map<String, String> cliMapping = cliMapping();
        for (String arg : args) {
            if (!arg.startsWith("--") || !arg.contains("=")) {
                throw new IllegalArgumentException("Parametro CLI invalido: " + arg + ". Usar formato --clave=valor");
            }
            String[] parts = arg.substring(2).split("=", 2);
            String propertyKey = cliMapping.get(parts[0]);
            if (propertyKey == null) {
                throw new IllegalArgumentException("Parametro CLI desconocido: --" + parts[0]);
            }
            properties.setProperty(propertyKey, parts.length == 2 ? parts[1] : "");
        }
    }

    private static Map<String, String> cliMapping() {
        Map<String, String> mapping = new HashMap<>();
        mapping.put("n", "simulation.n");
        mapping.put("l", "simulation.l");
        mapping.put("m", "simulation.m");
        mapping.put("rc", "simulation.rc");
        mapping.put("radius-min", "simulation.radius.min");
        mapping.put("radius-max", "simulation.radius.max");
        mapping.put("periodic", "simulation.periodic");
        mapping.put("random-seed", "simulation.random-seed");
        mapping.put("input-mode", "simulation.input-mode");
        mapping.put("static-file", "simulation.input.static");
        mapping.put("dynamic-file", "simulation.input.dynamic");
        mapping.put("neighbours-file", "simulation.output.neighbours");
        mapping.put("time-file", "simulation.output.time");
        return mapping;
    }

    private static int intProperty(Properties properties, String key) {
        try {
            return Integer.parseInt(properties.getProperty(key));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("La propiedad " + key + " debe ser entera", e);
        }
    }

    private static double doubleProperty(Properties properties, String key) {
        try {
            return Double.parseDouble(properties.getProperty(key));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("La propiedad " + key + " debe ser numerica", e);
        }
    }

    private static boolean booleanProperty(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            throw new IllegalArgumentException("La propiedad " + key + " debe ser true o false");
        }
        return Boolean.parseBoolean(value);
    }

    private static OptionalLong optionalLongProperty(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return OptionalLong.empty();
        }
        try {
            return OptionalLong.of(Long.parseLong(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("La propiedad " + key + " debe ser long o vacia", e);
        }
    }
}
