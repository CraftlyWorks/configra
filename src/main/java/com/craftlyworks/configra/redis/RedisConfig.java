package com.craftlyworks.configra.redis;

import com.craftlyworks.configra.config.ConfigKey;
import com.craftlyworks.configra.config.ConfigRegistry;
import com.craftlyworks.configra.config.IConfigSource;
import com.craftlyworks.configra.util.YamlUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class RedisConfig {
    public static final ConfigRegistry CONFIG = new ConfigRegistry();
    public static final ConfigKey<String> HOST = CONFIG.add("redis.host", "localhost", String.class);
    public static final ConfigKey<Integer> PORT = CONFIG.add("redis.port", 6379, Integer.class);
    public static final ConfigKey<String> PREFIX = CONFIG.add("redis.prefix", "", String.class);
    public static final ConfigKey<Boolean> LOCAL_ENV = CONFIG.add("local.environment", false, Boolean.class);

    /**
     * Loads the Redis configuration from a YAML file.
     *
     * @param file The configuration file.
     * @throws IOException If the file could not be read.
     */
    public static void load(@NotNull File file) throws IOException {
        Objects.requireNonNull(file, "file cannot be null");
        Redis.INSTANCE.load(YamlUtil.load(file));
    }

    /**
     * Loads the Redis configuration from a config source.
     *
     * @param source The config source.
     */
    public static void load(@NotNull IConfigSource source) {
        Objects.requireNonNull(source, "source cannot be null");
        Redis.INSTANCE.load(source);
    }
}
