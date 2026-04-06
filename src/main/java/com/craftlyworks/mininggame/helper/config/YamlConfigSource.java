package com.craftlyworks.mininggame.helper.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

public class YamlConfigSource implements IConfigSource {
    private final @NotNull Map<String, Object> data;

    public YamlConfigSource(@NotNull Map<String, Object> data) {
        //---- Validation ----//
        Objects.requireNonNull(data, "data cannot be null");
        //---- Set the data ----//
        this.data = data;
    }

    @Override
    public @Nullable Object get(@NotNull String key) {
        //---- Validation ----//
        Objects.requireNonNull(key, "key cannot be null");
        //---- Getting value from yaml ----//
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
