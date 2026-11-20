package com.helpdesk;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.context.annotation.Import(TestFixtures.class)
public abstract class SqliteTestSupport {
    protected static final Path DATABASE = createDatabase();

    private static Path createDatabase() {
        try {
            Path directory = Path.of("target", "test-databases");
            Files.createDirectories(directory);
            return Files.createTempFile(directory, "integration-", ".db").toAbsolutePath();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("helpdesk.database", DATABASE::toString);
    }
}
