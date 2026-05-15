package com.craftlyworks.configra.redis;

import com.craftlyworks.configra.config.IConfigSource;
import io.lettuce.core.KeyValue;
import io.lettuce.core.RedisClient;
import io.lettuce.core.ScoredValue;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;

public class Redis {
    public static final @NotNull Redis INSTANCE = new Redis();
    private final @NotNull Map<String, List<BiConsumer<String, String>>> listeners = new HashMap<>();
    private final @NotNull Map<String, Map<String, Double>> zsets = new HashMap<>();
    private final @NotNull Map<String, Map<String, String>> hashes = new HashMap<>();
    private final @NotNull Map<String, Set<String>> sets = new HashMap<>();
    private final @NotNull Map<String, String> strings = new HashMap<>();
    private @Nullable RedisClient client;
    private @Nullable StatefulRedisConnection<String, String> connection;
    @Getter
    private @Nullable RedisCommands<String, String> commands;
    private @Nullable StatefulRedisPubSubConnection<String, String> pubSubConnection;
    @Getter
    private @NotNull String prefix = "";
    // Fallback data structures
    private boolean useFallback = false;

    private Redis() {
    }

    public void load(@NotNull IConfigSource configSource) {
        Objects.requireNonNull(configSource, "configSource cannot be null");
        RedisConfig.CONFIG.validate(configSource);

        this.prefix = RedisConfig.CONFIG.get(configSource, RedisConfig.PREFIX);
        String redisHost = RedisConfig.CONFIG.get(configSource, RedisConfig.HOST);
        int redisPort = RedisConfig.CONFIG.get(configSource, RedisConfig.PORT);
        boolean isLocal = RedisConfig.CONFIG.get(configSource, RedisConfig.LOCAL_ENV);

        String redisUri = String.format("redis://%s:%d", redisHost, redisPort);

        try {
            client = RedisClient.create(redisUri);
            connection = client.connect();
            commands = connection.sync();

            // PubSub setup
            pubSubConnection = client.connectPubSub();
            pubSubConnection.addListener(new RedisPubSubListener<>() {
                @Override
                public void message(String channel, String message) {
                    List<BiConsumer<String, String>> channelListeners = listeners.get(channel);
                    if (channelListeners != null) {
                        for (BiConsumer<String, String> listener : channelListeners) {
                            listener.accept(channel, message);
                        }
                    }
                }

                @Override
                public void message(String pattern, String channel, String message) {
                }

                @Override
                public void subscribed(String channel, long count) {
                }

                @Override
                public void psubscribed(String pattern, long count) {
                }

                @Override
                public void unsubscribed(String channel, long count) {
                }

                @Override
                public void punsubscribed(String pattern, long count) {
                }
            });

            ping();
        } catch (Exception e) {
            if (isLocal) {
                useFallback = true;
                System.out.println("[Redis] Failed to connect, using fallback HashMap storage.");
            } else {
                throw e;
            }
        }
    }

    public boolean isConnected() {
        return useFallback || (client != null && connection != null && connection.isOpen());
    }

    public void unload() {
        if (pubSubConnection != null) {
            pubSubConnection.close();
        }
        if (connection != null) {
            connection.close();
        }
        if (client != null) {
            client.shutdown();
        }
    }

    public @NotNull String wrap(@NotNull String key) {
        Objects.requireNonNull(key, "key cannot be null");
        return prefix + key;
    }

    public void subscribe(@NotNull String channel, @NotNull BiConsumer<String, String> listener) {
        Objects.requireNonNull(channel, "channel cannot be null");
        Objects.requireNonNull(listener, "listener cannot be null");
        this.subscribe(channel, false, listener);
    }

    public void subscribeGlobal(@NotNull String channel, @NotNull BiConsumer<String, String> listener) {
        Objects.requireNonNull(channel, "channel cannot be null");
        Objects.requireNonNull(listener, "listener cannot be null");
        this.subscribe(channel, true, listener);
    }

    private void subscribe(@NotNull String channel, boolean global, @NotNull BiConsumer<String, String> listener) {
        Objects.requireNonNull(channel, "channel cannot be null");
        Objects.requireNonNull(listener, "listener cannot be null");
        String wrappedChannel = global ? channel : wrap(channel);
        listeners.computeIfAbsent(wrappedChannel, k -> {
            if (!useFallback && pubSubConnection != null) {
                pubSubConnection.sync().subscribe(wrappedChannel);
            }
            return new ArrayList<>();
        }).add(listener);
    }

    public void publish(@NotNull String channel, @NotNull String message) {
        Objects.requireNonNull(channel, "channel cannot be null");
        Objects.requireNonNull(message, "message cannot be null");
        this.publish(channel, false, message);
    }

    public void publishGlobal(@NotNull String channel, @NotNull String message) {
        Objects.requireNonNull(channel, "channel cannot be null");
        Objects.requireNonNull(message, "message cannot be null");
        this.publish(channel, true, message);
    }

    private void publish(@NotNull String channel, boolean global, @NotNull String message) {
        Objects.requireNonNull(channel, "channel cannot be null");
        Objects.requireNonNull(message, "message cannot be null");
        String wrappedChannel = global ? channel : wrap(channel);
        if (useFallback) {
            List<BiConsumer<String, String>> channelListeners = listeners.get(wrappedChannel);
            if (channelListeners != null) {
                for (BiConsumer<String, String> listener : channelListeners) {
                    listener.accept(wrappedChannel, message);
                }
            }
            return;
        }
        if (commands != null) {
            commands.publish(wrappedChannel, message);
        }
    }

    public void ping() {
        if (useFallback) return;
        if (commands == null) return;
        String pong = commands.ping();
        if (!"PONG".equals(pong)) {
            throw new IllegalStateException("Redis ping failed.");
        }
    }

    // Set and get ZSET (leaderboards)
    public void zaddOne(@NotNull String key, long score, @NotNull String member) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(member, "member cannot be null");
        String wrappedKey = wrap(key);
        if (useFallback) {
            zsets.computeIfAbsent(wrappedKey, k -> new HashMap<>()).put(member, (double) score);
            return;
        }
        if (commands != null) {
            commands.zadd(wrappedKey, (double) score, member);
        }
    }

    public <V extends Number> void zaddMany(@NotNull String key, @NotNull Map<String, V> scoreMembers) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(scoreMembers, "scoreMembers cannot be null");
        if (scoreMembers.isEmpty()) {
            return;
        }
        String wrappedKey = wrap(key);
        if (useFallback) {
            Map<String, Double> zset = zsets.computeIfAbsent(wrappedKey, k -> new HashMap<>());
            scoreMembers.forEach((member, score) -> zset.put(member, score.doubleValue()));
            return;
        }
        if (commands != null) {
            //noinspection unchecked
            ScoredValue<String>[] values = new ScoredValue[scoreMembers.size()];
            int i = 0;
            for (Map.Entry<String, V> entry : scoreMembers.entrySet()) {
                values[i++] = ScoredValue.just(entry.getValue().doubleValue(), entry.getKey());
            }
            commands.zadd(wrappedKey, values);
        }
    }

    public void zremOne(@NotNull String key, @NotNull String member) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(member, "member cannot be null");
        String wrappedKey = wrap(key);
        if (useFallback) {
            Map<String, Double> zset = zsets.get(wrappedKey);
            if (zset != null) {
                zset.remove(member);
            }
            return;
        }
        if (commands != null) {
            commands.zrem(wrappedKey, member);
        }
    }

    public @NotNull List<String> zrangeAll(@NotNull String key) {
        Objects.requireNonNull(key, "key cannot be null");
        String wrappedKey = wrap(key);
        if (useFallback) {
            Map<String, Double> zset = zsets.get(wrappedKey);
            if (zset == null) return Collections.emptyList();
            return zset.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();
        }
        if (commands != null) {
            return commands.zrevrange(wrappedKey, 0, -1);
        }
        return Collections.emptyList();
    }

    // Hash helpers for storing formatted strings (e.g., uuid -> formatted line)
    public void hset(@NotNull String key, @NotNull String field, @NotNull String value) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(field, "field cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        String wrappedKey = wrap(key);
        if (useFallback) {
            hashes.computeIfAbsent(wrappedKey, k -> new HashMap<>()).put(field, value);
            return;
        }
        if (commands != null) {
            commands.hset(wrappedKey, field, value);
        }
    }

    public void hsetMany(@NotNull String key, @NotNull Map<String, String> fieldValues) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(fieldValues, "fieldValues cannot be null");
        if (fieldValues.isEmpty()) {
            return;
        }
        String wrappedKey = wrap(key);
        if (useFallback) {
            hashes.computeIfAbsent(wrappedKey, k -> new HashMap<>()).putAll(fieldValues);
            return;
        }
        if (commands != null) {
            commands.hset(wrappedKey, fieldValues);
        }
    }

    public @NotNull Map<String, String> hmget(@NotNull String key, @NotNull List<String> fields) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(fields, "fields cannot be null");
        if (fields.isEmpty()) {
            return Collections.emptyMap();
        }
        String wrappedKey = wrap(key);
        if (useFallback) {
            Map<String, String> hash = hashes.get(wrappedKey);
            Map<String, String> result = new HashMap<>();
            for (String field : fields) {
                result.put(field, hash != null ? hash.get(field) : null);
            }
            return result;
        }
        if (commands != null) {
            String[] arr = fields.toArray(new String[0]);
            List<KeyValue<String, String>> kvs = commands.hmget(wrappedKey, arr);
            Map<String, String> result = new HashMap<>();
            for (KeyValue<String, String> kv : kvs) {
                if (kv == null) continue;
                String f = kv.getKey();
                if (kv.hasValue()) {
                    result.put(f, kv.getValue());
                } else {
                    result.put(f, null);
                }
            }
            return result;
        }
        return Collections.emptyMap();
    }

    public @Nullable String hget(@NotNull String key, @NotNull String field) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(field, "field cannot be null");
        String wrappedKey = wrap(key);
        if (useFallback) {
            Map<String, String> hash = hashes.get(wrappedKey);
            return hash != null ? hash.get(field) : null;
        }
        if (commands != null) {
            return commands.hget(wrappedKey, field);
        }
        return null;
    }

    public void set(@NotNull String key, @NotNull String value) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        String wrappedKey = wrap(key);
        if (useFallback) {
            strings.put(wrappedKey, value);
            return;
        }
        if (commands != null) {
            commands.set(wrappedKey, value);
        }
    }

    public @Nullable String get(@NotNull String key) {
        Objects.requireNonNull(key, "key cannot be null");
        String wrappedKey = wrap(key);
        if (useFallback) {
            return strings.get(wrappedKey);
        }
        if (commands != null) {
            return commands.get(wrappedKey);
        }
        return null;
    }

    public void del(@NotNull String key) {
        Objects.requireNonNull(key, "key cannot be null");
        String wrappedKey = wrap(key);
        if (useFallback) {
            strings.remove(wrappedKey);
            zsets.remove(wrappedKey);
            hashes.remove(wrappedKey);
            sets.remove(wrappedKey);
            return;
        }
        if (commands != null) {
            commands.del(wrappedKey);
        }
    }

    // Set operations
    public void sadd(@NotNull String key, @NotNull String... members) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(members, "members cannot be null");
        String wrappedKey = wrap(key);
        if (useFallback) {
            Collections.addAll(sets.computeIfAbsent(wrappedKey, k -> new HashSet<>()), members);
            return;
        }
        if (commands != null) {
            commands.sadd(wrappedKey, members);
        }
    }

    public void srem(@NotNull String key, @NotNull String... members) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(members, "members cannot be null");
        String wrappedKey = wrap(key);
        if (useFallback) {
            Set<String> set = sets.get(wrappedKey);
            if (set != null) {
                for (String member : members) {
                    set.remove(member);
                }
            }
            return;
        }
        if (commands != null) {
            commands.srem(wrappedKey, members);
        }
    }

    public @NotNull Set<String> smembers(@NotNull String key) {
        Objects.requireNonNull(key, "key cannot be null");
        String wrappedKey = wrap(key);
        if (useFallback) {
            Set<String> set = sets.get(wrappedKey);
            return set != null ? new HashSet<>(set) : Collections.emptySet();
        }
        if (commands != null) {
            return commands.smembers(wrappedKey);
        }
        return Collections.emptySet();
    }

    public @Nullable String setNx(@NotNull String key, @NotNull String value, long expiryMillis) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        String wrappedKey = wrap(key);
        if (useFallback) {
            // Very basic fallback, doesn't handle NX/PX properly but good enough for local
            hashes.computeIfAbsent("locks", k -> new HashMap<>()).put(wrappedKey, value);
            return "OK";
        }
        if (commands != null) {
            return commands.set(wrappedKey, value, SetArgs.Builder.nx().px(expiryMillis));
        }
        return null;
    }

}
