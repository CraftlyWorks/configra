package com.craftlyworks.mininggame.helper.mongo;

import com.craftlyworks.mininggame.helper.config.ConfigKey;
import com.craftlyworks.mininggame.helper.config.ConfigRegistry;
import com.craftlyworks.mininggame.helper.config.IConfigSource;
import com.craftlyworks.mininggame.helper.util.YamlUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class MongoConfig {
    public static final ConfigRegistry CONFIG = new ConfigRegistry();
    public static final ConfigKey<String> URI = CONFIG.add("mongo.uri", String.class);
    public static final ConfigKey<String> DATABASE = CONFIG.add("mongo.database", String.class);
    public static final ConfigKey<Boolean> LOCAL_ENV = CONFIG.add("local.environment", false, Boolean.class);

    /**
     * Loads the Mongo configuration from a YAML file.
     *
     * @param file The configuration file.
     * @throws IOException If the file could not be read.
     */
    public static void load(@NotNull File file) throws IOException {
        //---- Validation ----//
        Objects.requireNonNull(file, "file cannot be null");
        //---- Loading config from file ----//
        Mongo.INSTANCE.load(YamlUtil.load(file));
    }

    /**
     * Loads the Mongo configuration from a config source.
     *
     * @param source The config source.
     */
    public static void load(@NotNull IConfigSource source) {
        //---- Validation ----//
        Objects.requireNonNull(source, "source cannot be null");
        //---- Loading config from source ----//
        Mongo.INSTANCE.load(source);
    }
}
