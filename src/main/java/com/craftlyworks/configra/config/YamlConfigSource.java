package com.craftlyworks.configra.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

public class YamlConfigSource implements IConfigSource {
    private final @NotNull Map<String, Object> data;

    public YamlConfigSource(@NotNull Map<String, Object> data) {
        Objects.requireNonNull(data, "data cannot be null");
        this.data = data;
    }

    @Override
    public @Nullable Object get(@NotNull String key) {
        Objects.requireNonNull(key, "key cannot be null");
        if (key.isEmpty()) return null;
        String[] parts = key.split("\\.");
        Object current = data;
        for (String part : parts) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<?, ?>) current).get(part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }
}
