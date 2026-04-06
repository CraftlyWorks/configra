package com.craftlyworks.configra.config;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@Getter
public class ConfigKey<T> {
    private final @NotNull String key;
    private final @Nullable T defaultValue;
    private final boolean required;
    private final @NotNull Class<T> type;

    private ConfigKey(@NotNull String key, @Nullable T defaultValue, boolean required, @NotNull Class<T> type) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        this.key = key;
        this.defaultValue = defaultValue;
        this.required = required;
        this.type = type;
    }

    public static <T> @NotNull ConfigKey<T> of(@NotNull String key, @NotNull Class<T> type) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        return new ConfigKey<>(key, null, true, type);
    }

    public static <T> @NotNull ConfigKey<T> of(@NotNull String key, @NotNull T defaultValue, @NotNull Class<T> type) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(defaultValue, "defaultValue cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        return new ConfigKey<>(key, defaultValue, false, type);
    }

    public @Nullable T cast(@Nullable Object value) {
        if (value == null) return null;
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        // Basic conversion for common types if needed, though usually, we expect the right type from the source
        if (type == Integer.class && value instanceof Number) {
            return type.cast(((Number) value).intValue());
        }
        if (type == Long.class && value instanceof Number) {
            return type.cast(((Number) value).longValue());
        }
        if (type == Double.class && value instanceof Number) {
            return type.cast(((Number) value).doubleValue());
        }
        throw new ClassCastException("Config key '" + key + "' expected " + type.getSimpleName() + " but got " + value.getClass().getSimpleName());
    }
}
