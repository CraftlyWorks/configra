package com.craftlyworks.configra.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The commands that close the gaps this class used to have: a hash you could write but never
 * remove from or read whole, and no way to give any key a lifetime except by never refreshing it.
 *
 * <p>Driven against a real server, because what is being checked is the wire behaviour - what
 * Redis returns for a field that has already gone, whether the per-field expiry a shared hash
 * depends on is supported at all - and a mock would answer whatever it was told to. Where no
 * server is reachable the tests skip rather than fail: this is a library, and its build must not
 * require infrastructure.
 *
 * <p>Every key is namespaced under a random id and deleted afterwards, so a run leaves nothing
 * behind even against a Redis holding real data.
 */
class RedisCommandsTest {

    private static RedisClient client;
    private static StatefulRedisConnection<String, String> connection;
    private static RedisCommands<String, String> redis;
    private static String ns;

    @BeforeAll
    static void connect() {
        try {
            client = RedisClient.create("redis://127.0.0.1:6379");
            connection = client.connect();
            redis = connection.sync();
            redis.ping();
        } catch (RuntimeException unreachable) {
            redis = null;
        }
        assumeTrue(redis != null, "no Redis on 127.0.0.1:6379 - skipping");
        ns = "configra:test:" + UUID.randomUUID() + ":";
    }

    @AfterAll
    static void cleanUp() {
        if (redis != null && ns != null) {
            redis.keys(ns + "*").forEach(redis::del);
        }
        if (connection != null) {
            connection.close();
        }
        if (client != null) {
            client.shutdown();
        }
    }

    @Test
    void hdelRemovesOnlyTheFieldsNamed() {
        String key = ns + "hash";
        redis.hset(key, Map.of("a", "1", "b", "2", "c", "3"));

        assertEquals(2L, redis.hdel(key, "a", "b"));
        assertEquals(Map.of("c", "3"), redis.hgetall(key));
        // Removing what is already gone is not an error, which is what makes a delete safe to retry.
        assertEquals(0L, redis.hdel(key, "a"));
    }

    @Test
    void hgetallAnswersAboutFieldsNobodyKnewTheNamesOf() {
        String key = ns + "roster";
        redis.hset(key, Map.of("alice", "prod1", "bob", "prod2"));

        Map<String, String> all = redis.hgetall(key);
        assertEquals(2, all.size());
        assertEquals("prod1", all.get("alice"));
        // The point of the command: hmget needs the names up front, and a roster is exactly the
        // case where the membership is the question.
        assertTrue(all.keySet().containsAll(List.of("alice", "bob")));
    }

    @Test
    void hgetallOnAMissingKeyIsEmptyRatherThanNull() {
        assertEquals(Map.of(), redis.hgetall(ns + "never-written"));
    }

    @Test
    void expireGivesAKeyALifetime() {
        String key = ns + "ttl";
        redis.set(key, "v");

        assertTrue(redis.expire(key, 60));
        Long ttl = redis.ttl(key);
        assertNotNull(ttl);
        assertTrue(ttl > 0 && ttl <= 60, "ttl was " + ttl);
    }

    @Test
    void expireOnAKeyThatIsNotThereReportsThatItDidNothing() {
        // The distinction matters: a caller refreshing a heartbeat needs to know the key it
        // believed in has gone, not to be told the refresh worked.
        assertFalse(redis.expire(ns + "absent", 60));
    }

    /**
     * The command a shared hash needs. A key-level TTL takes the whole hash at once, so several
     * writers into one key cannot each be responsible for their own entries - refreshing yours
     * keeps everybody's dead ones alive too.
     */
    @Test
    void hexpireGivesIndividualFieldsTheirOwnLifetime() {
        String key = ns + "shared";
        redis.hset(key, Map.of("mine", "1", "yours", "2"));

        List<Long> statuses = redis.hexpire(key, 60, "mine");
        assertNotNull(statuses, "needs Redis 7.4+; older servers do not have HEXPIRE");
        assertEquals(List.of(1L), statuses);

        List<Long> ttls = redis.httl(key, "mine", "yours");
        assertNotNull(ttls);
        assertTrue(ttls.get(0) > 0, "the field we set should be counting down");
        // -1 is "no expiry": the other writer's field is untouched, which is the whole point.
        assertEquals(-1L, ttls.get(1));
    }

    @Test
    void hexpireOnAFieldThatIsNotThereSaysSo() {
        String key = ns + "sparse";
        redis.hset(key, "present", "1");

        // -2 is Redis's "no such field". Reporting it rather than treating it as success is what
        // lets a caller notice it is expiring something it no longer owns.
        assertEquals(List.of(-2L), redis.hexpire(key, 60, "absent"));
    }
}
