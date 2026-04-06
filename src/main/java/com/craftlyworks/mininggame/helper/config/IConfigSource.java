package com.craftlyworks.mininggame.helper.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IConfigSource {
    @Nullable Object get(@NotNull String key);
}
