package com.craftlyworks.configra.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IConfigSource {
    @Nullable Object get(@NotNull String key);
}
