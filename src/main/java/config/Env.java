package config;

import logging.Debug;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Env {
    private List<String> lines;

    public Env() throws IOException {
        lines = initLines();
    }

    private List<String> initLines() throws IOException {
        Path filePath = Paths.get(".env");
        Path exampleFilePath = Paths.get(".env.example");

        if (!Files.exists(filePath)) Files.copy(exampleFilePath, filePath);
        return Files.readAllLines(filePath);

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