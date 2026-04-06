package com.craftlyworks.configra.mininggame.config;

import com.craftlyworks.configra.config.YamlConfigSource;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class YamlConfigSourceTest {
    @Test
    public void testNestedKeys() {
        Map<String, Object> data = Map.of(
            "redis", Map.of(
                "host", "localhost",
                "port", 6379
            ),
            "mongo", Map.of(
                "uri", "mongodb://localhost:27017"
            )
        );

        YamlConfigSource source = new YamlConfigSource(data);

        assertEquals("localhost", source.get("redis.host"));
        assertEquals(6379, source.get("redis.port"));
        assertEquals("mongodb://localhost:27017", source.get("mongo.uri"));
        assertNull(source.get("redis.nonexistent"));
        assertNull(source.get("mongo.database"));
    }
}
