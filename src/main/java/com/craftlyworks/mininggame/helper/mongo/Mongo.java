package com.craftlyworks.mininggame.helper.mongo;

import com.craftlyworks.mininggame.helper.config.IConfigSource;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import org.bson.UuidRepresentation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class Mongo {
    public static @NotNull Mongo INSTANCE = new Mongo();
    private @Nullable MongoClient mongoClient;
    private @Nullable String databaseName;
    @Getter
    private boolean useFallback = false;

    private Mongo() {

    }

    public void load(@NotNull IConfigSource configSource) {
        //---- Validation ----//
        Objects.requireNonNull(configSource, "configSource cannot be null");
        //---- Initialize the mongo ----//
        if (mongoClient != null) {
            throw new IllegalStateException("Mongo has already been initialized.");
        }
        MongoConfig.CONFIG.validate(configSource);
        String mongoUri = MongoConfig.CONFIG.get(configSource, MongoConfig.URI);
        this.databaseName = MongoConfig.CONFIG.get(configSource, MongoConfig.DATABASE);
        //noinspection DataFlowIssue
        boolean isLocal = MongoConfig.CONFIG.get(configSource, MongoConfig.LOCAL_ENV);
        try {
            var clientSettings = MongoClientSettings.builder()
                .uuidRepresentation(UuidRepresentation.JAVA_LEGACY)
                .applyConnectionString(new ConnectionString(mongoUri));
            if (isLocal) {
                clientSettings.timeout(1, TimeUnit.SECONDS);
            }
            mongoClient = MongoClients.create(clientSettings.build());
            // Verify connection
            mongoClient.listDatabaseNames().first();
        } catch (Exception e) {
            if (isLocal) {
                useFallback = true;
                System.out.println("[Mongo] Failed to connect, using fallback HashMap storage.");
            } else {
                throw e;
            }
        }
    }

    public @Nullable MongoDatabase getDatabase() {
        //---- Getting database ----//
        if (useFallback) {
            return null;
        }
        if (mongoClient == null) {
            throw new IllegalStateException("Mongo has not been initialized.");
        }
        return mongoClient.getDatabase(databaseName);
    }

    public void unload() {
        //---- Unloading the mongo ----//
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}
