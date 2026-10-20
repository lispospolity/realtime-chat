package app.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Env {
    private final List<String> lines;
    private Path env = Paths.get(".env");
    private Path exampleEnv = Paths.get(".env.example");

    public Env() throws IOException {
        lines = initLines();
    }
    public Env(Path env, Path exampleEnv) throws IOException {
        this.env = env;
        this.exampleEnv = exampleEnv;
        lines = initLines();
    }

    private List<String> initLines() throws IOException {
        if (!Files.exists(env)) Files.copy(exampleEnv, env);

        return Files.readAllLines(env);
    }

    public String get(String prompt) {
        for (String line : lines) {
            if (line.startsWith("#")) continue;
            if (!line.contains("=")) continue;

            String[] parts = line.split("=", 2);
            if (parts[0].equals(prompt)) return parts[1];
        }
        return "";
    }

    public String getRequired(String prompt) {
        String value = get(prompt);

        if (value.isBlank()) throw new IllegalStateException(
                "Missing required configuration field in " + prompt
        );
        return value;
    }

    public String getOrDefault(String prompt, String def) {
        String response = get(prompt);
        return response.isBlank() ? def : response;
    }
}