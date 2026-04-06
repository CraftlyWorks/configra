package com.craftlyworks.configra.util;

import com.craftlyworks.configra.config.YamlConfigSource;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;

public class YamlUtil {
    private static final Yaml YAML = new Yaml();

    /**
     * Loads a YAML file and returns it as a YamlConfigSource.
     * If the file doesn't exist, it will be created.
     *
     * @param file The file to load.
     * @return A YamlConfigSource containing the file data.
     * @throws IOException If the file could not be created or read.
     */
    public static @NotNull YamlConfigSource load(@NotNull File file) throws IOException {
        Objects.requireNonNull(file, "file cannot be null");
        if (!file.exists()) {
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                if (!file.getParentFile().mkdirs()) {
                    throw new IOException("Could not create parent directories for " + file.getName());
                }
            }
            if (!file.createNewFile()) {
                throw new IOException("Could not create " + file.getName());
            }
        }
        Map<String, Object> data;
        try (var reader = Files.newBufferedReader(file.toPath())) {
            data = YAML.load(reader);
        }
        return new YamlConfigSource(data == null ? Map.of() : data);
    }

    /**
     * Loads a YAML file from a directory and filename.
     *
     * @param folder   The folder containing the file.
     * @param fileName The name of the file.
     * @return A YamlConfigSource containing the file data.
     * @throws IOException If the file could not be created or read.
     */
    public static @NotNull YamlConfigSource load(@NotNull File folder, @NotNull String fileName) throws IOException {
        Objects.requireNonNull(folder, "folder cannot be null");
        Objects.requireNonNull(fileName, "fileName cannot be null");
        return load(new File(folder, fileName));
    }
}
