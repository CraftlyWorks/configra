package com.craftlyworks.configra.redis;

import com.craftlyworks.configra.config.YamlConfigSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The same commands, but through {@link Redis} rather than through lettuce.
 *
 * <p>This is the half that actually ships. {@code RedisCommandsTest} proves what a real server
 * does - that HEXPIRE exists, what it returns for a field that is not there - and this proves the
 * wrapper reads those answers correctly, which is a separate thing and the one callers depend on.
 * The status codes in particular are easy to get backwards: a caller told its expiry succeeded
 * when it did not gets a key that lives forever.
 *
 * <p>Skips where no server is reachable, for the same reason: a library's build must not require
 * infrastructure.
 */
class RedisWrapperTest {

    private static boolean connected;
    private static String ns;

    @BeforeAll
    static void connect() {
        ns = "configra-wrapper-test-" + UUID.randomUUID() + ":";
        try {
            Redis.INSTANCE.load(new YamlConfigSource(Map.of(
                "redis", Map.of("host", "127.0.0.1", "port", 6379, "prefix", ns),
                "local", Map.of("environment", false)
            )));
            connected = Redis.INSTANCE.isConnected();
        } catch (RuntimeException unreachable) {
            connected = false;
        }
        assumeTrue(connected, "no Redis on 127.0.0.1:6379 - skipping");
    }

    @AfterAll
    static void cleanUp() {
        if (connected) {
            Redis.INSTANCE.del("hash");
            Redis.INSTANCE.del("ttl");
            Redis.INSTANCE.unload();
        }
    }

    @Test
    void hgetAllAndHdelGoThroughTheWrapper() {
        Redis.INSTANCE.hset("hash", "alice", "prod1");
        Redis.INSTANCE.hset("hash", "bob", "prod2");

        assertEquals(Map.of("alice", "prod1", "bob", "prod2"), Redis.INSTANCE.hgetAll("hash"));

        assertEquals(1L, Redis.INSTANCE.hdel("hash", "alice"));
        assertEquals(Map.of("bob", "prod2"), Redis.INSTANCE.hgetAll("hash"));
        assertEquals(0L, Redis.INSTANCE.hdel("hash", "alice"), "removing what is gone is not an error");
    }

    @Test
    void expireReportsWhetherItActuallySetOne() {
        Redis.INSTANCE.set("ttl", "v");
        assertTrue(Redis.INSTANCE.expire("ttl", 60));
        // A key that is not there cannot be given a lifetime, and saying otherwise would tell a
        // caller its heartbeat landed when the thing it was refreshing has gone.
        assertFalse(Redis.INSTANCE.expire("definitely-absent", 60));
    }

    @Test
    void hexpireReportsSuccessOnlyWhenTheFieldWasThere() {
        Redis.INSTANCE.hset("hash", "present", "1");

        assertTrue(Redis.INSTANCE.hexpire("hash", 60, "present"));
        // -2 from Redis means "no such field". Reading that as success is the mistake that leaves
        // a caller believing an entry it no longer owns will expire.
        assertFalse(Redis.INSTANCE.hexpire("hash", 60, "absent"));
        assertFalse(Redis.INSTANCE.hexpire("hash", 60), "no fields is not a success");
    }

    @Test
    void existsSeesEachKindOfKey() {
        Redis.INSTANCE.set("ttl", "v");
        assertTrue(Redis.INSTANCE.exists("ttl"));
        assertFalse(Redis.INSTANCE.exists("never-written"));
    }

    @Test
    void aFieldGivenALifetimeIsGoneAfterItPasses() throws InterruptedException {
        Redis.INSTANCE.hset("hash", "fleeting", "1");
        assertTrue(Redis.INSTANCE.hexpire("hash", 1, "fleeting"));
        assertNull(waitForRemoval("hash", "fleeting"), "the field should have expired");
    }

    /** Polls rather than sleeping a fixed time, so a slow machine does not fail the run. */
    private static String waitForRemoval(String key, String field) throws InterruptedException {
        for (int i = 0; i < 40; i++) {
            if (Redis.INSTANCE.hget(key, field) == null) return null;
            Thread.sleep(100);
        }
        return Redis.INSTANCE.hget(key, field);
    }
}
