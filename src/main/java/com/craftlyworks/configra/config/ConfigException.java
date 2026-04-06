package com.craftlyworks.configra.config;

import org.jetbrains.annotations.NotNull;

public class ConfigException extends RuntimeException {
    public ConfigException(@NotNull String message) {
        super(message);
    }
}
