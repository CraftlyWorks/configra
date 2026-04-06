package com.craftlyworks.configra.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConfigRegistry {
    private final @NotNull List<@NotNull ConfigKey<?>> keys = new ArrayList<>();

    public void register(@NotNull ConfigKey<?> key) {
        Objects.requireNonNull(key, "key cannot be null");
        keys.add(key);
    }

    public <T> @NotNull ConfigKey<T> add(@NotNull String key, @NotNull Class<T> type) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        ConfigKey<T> configKey = ConfigKey.of(key, type);
        register(configKey);
        return configKey;
    }

    public <T> @NotNull ConfigKey<T> add(@NotNull String key, @NotNull T defaultValue, @NotNull Class<T> type) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        @NotNull ConfigKey<T> configKey = ConfigKey.of(key, defaultValue, type);
        register(configKey);
        return configKey;
    }

    public void validate(@NotNull IConfigSource source) {
        Objects.requireNonNull(source, "source cannot be null");
        for (@NotNull ConfigKey<?> key : keys) {
            @Nullable Object value = source.get(key.getKey());
            if (value == null && key.isRequired() && key.getDefaultValue() == null) {
                throw new ConfigException("Missing required configuration key: " + key.getKey());
            }
        }
    }

    public <T> @NotNull T getOrDefault(@NotNull IConfigSource source, @NotNull ConfigKey<T> key, @NotNull T defaultValue) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(defaultValue, "defaultValue cannot be null");
        @Nullable Object value = this.get(source, key);
        if (value == null) {
            return defaultValue;
        } else {
            //noinspection DataFlowIssue
            return key.cast(value);
        }
    }

    public <T> @Nullable T get(@NotNull IConfigSource source, @NotNull ConfigKey<T> key) {
        Objects.requireNonNull(source, "source cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
        @Nullable Object value = source.get(key.getKey());
        if (value == null) {
            if (key.getDefaultValue() != null) {
                return key.getDefaultValue();
            }
            if (key.isRequired()) {
                throw new ConfigException("Missing required configuration key: " + key.getKey());
            }
            return null;
        }
        return key.cast(value);
    }
}
