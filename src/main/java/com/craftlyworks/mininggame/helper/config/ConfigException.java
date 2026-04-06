package com.craftlyworks.mininggame.helper.config;

import org.jetbrains.annotations.NotNull;

public class ConfigException extends RuntimeException {
    public ConfigException(@NotNull String message) {
        //---- Validation ----//
        super(message);
    }
}
