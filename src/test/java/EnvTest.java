import config.Env;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class EnvTest {
    private final Path testEnvPath = Paths.get("src/test/resources/.env.test");
    private final Path testExampleEnvPath = Paths.get("src/test/resources/.env.example.test");

    private Env env;

    @BeforeEach
    void loadEnv() throws IOException {
        env = new Env(testEnvPath, testExampleEnvPath);
    }

    @Test
    void envGet() {
        Assertions.assertEquals(
                "hello_world",
                env.get("TESTING_VALUE"));
        Assertions.assertEquals(
                "",
                env.get("EMPTY_VALUE"));
        Assertions.assertEquals(
                "",
                env.get("COMMENT"));
        Assertions.assertEquals(
                "",
                env.get("WRITTEN_WRONG")
        );
    }

    @Test
    void envGetOrDefault() {
        Assertions.assertEquals(
                "hello_world",
                env.getOrDefault("TESTING_VALUE", "default_return"));
        Assertions.assertEquals(
                "default_return",
                env.getOrDefault("EMPTY_VALUE", "default_return"));
    }

    @Test
    void envGetRequired() {
        Assertions.assertEquals("hello_world", env.getRequired("TESTING_VALUE"));
        Assertions.assertThrows(
                IllegalStateException.class,
                () -> env.getRequired("EMPTY_VALUE"));
    }

    @Test
    void envLoadNonexistent(@TempDir Path tempDir) {
        Path mainEnv = tempDir.resolve(".env");
        Path exampleEnv = tempDir.resolve(".env.example");

        Assertions.assertThrows(
                IOException.class,
                () -> new Env(mainEnv, exampleEnv)
                );
    }

    @Test
    void envCopyExample(@TempDir Path tempDir) throws IOException {
        Path mainEnv = tempDir.resolve(".env");
        Path exampleEnv = tempDir.resolve(".env.example");

        Files.writeString(exampleEnv, "TESTING_VALUE=hello_world");

        Env env = new Env(mainEnv, exampleEnv);

        Assertions.assertEquals(-1, Files.mismatch(mainEnv, exampleEnv));
    }
}
