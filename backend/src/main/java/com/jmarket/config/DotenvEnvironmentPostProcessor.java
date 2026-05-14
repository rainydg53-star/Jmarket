package com.jmarket.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> values = new LinkedHashMap<>();
        loadIfExists(values, Path.of(".env"));
        loadIfExists(values, Path.of("..", ".env"));
        normalizeMysqlUrl(values, environment);

        if (!values.isEmpty()) {
            // Prioritize repository .env values over machine-level environment variables
            // so local Docker DB settings are used consistently.
            environment.getPropertySources().addFirst(new MapPropertySource("dotenv", values));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private void loadIfExists(Map<String, Object> target, Path path) {
        if (!Files.exists(path)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (String line : lines) {
                parseLine(line).ifPresent(entry -> target.put(entry.getKey(), entry.getValue()));
            }
        } catch (IOException ignored) {
            // If .env can't be read, fallback to existing environment/default values.
        }
    }

    private java.util.Optional<Map.Entry<String, String>> parseLine(String rawLine) {
        String line = rawLine.trim();
        if (!line.isEmpty() && line.charAt(0) == '\uFEFF') {
            line = line.substring(1).trim();
        }
        if (line.isEmpty() || line.startsWith("#")) {
            return java.util.Optional.empty();
        }

        if (line.startsWith("export ")) {
            line = line.substring("export ".length()).trim();
        }

        int separatorIndex = line.indexOf('=');
        if (separatorIndex <= 0) {
            return java.util.Optional.empty();
        }

        String key = line.substring(0, separatorIndex).trim();
        String value = line.substring(separatorIndex + 1).trim();
        if (key.isEmpty()) {
            return java.util.Optional.empty();
        }

        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            if (value.length() >= 2) {
                value = value.substring(1, value.length() - 1);
            }
        }

        return java.util.Optional.of(Map.entry(key, value));
    }

    private void normalizeMysqlUrl(Map<String, Object> values, ConfigurableEnvironment environment) {
        Object rawValue = values.getOrDefault("DB_URL", environment.getProperty("DB_URL"));
        if (!(rawValue instanceof String dbUrl)) {
            return;
        }
        if (!dbUrl.startsWith("jdbc:mysql:") || dbUrl.contains("allowPublicKeyRetrieval=")) {
            values.put("DB_URL", dbUrl);
            return;
        }

        String separator = dbUrl.contains("?") ? "&" : "?";
        values.put("DB_URL", dbUrl + separator + "allowPublicKeyRetrieval=true");
    }
}
