# Configra

A simple collection of Java utilities for managing configurations, MongoDB connections, and Redis operations.

## Table of Contents

- [Configuration Management](#configuration-management)
- [MongoDB Integration](#mongodb-integration)
- [Redis Integration](#redis-integration)
- [Building the Project](#building-the-project)
- [Contributing](#contributing)
- [Coding Style](#coding-style)

## Configuration Management

The library provides a flexible way to define and access configuration keys from various sources (currently supporting
YAML).

### Defining Keys

```java
ConfigRegistry registry = new ConfigRegistry();
// Required key (throws exception if missing and no default)
ConfigKey<String> MONGO_URI = registry.add("mongo.uri", String.class);
// Key with a default value (optional)
ConfigKey<Integer> REDIS_PORT = registry.add("redis.port", 6379, Integer.class);
```

### Loading from YAML

```java
File configFile = new File("config.yml");
IConfigSource source = YamlUtil.load(configFile);
// Validate that all required keys are present
registry.validate(source);
// Retrieve values
String uri = registry.get(source, MONGO_URI);
int port = registry.getOrDefault(source, REDIS_PORT, 6379);
```

## MongoDB Integration

Uses a singleton pattern for easy access and includes a fallback mechanism for local development.

### Initialization

```java
// Load from file (automatically uses MongoConfig keys)
MongoConfig.load(new File("config.yml"));
// Access database
MongoDatabase db = Mongo.INSTANCE.getDatabase();
```

### Fallback Mode

If `local.environment` is set to `true` and the connection fails, it will use a fallback (where applicable).

## Redis Integration

Supports Pub/Sub, ZSETs (leaderboards), and Hashes with prefix wrapping.

### Initialization

```java
// Load from file
RedisConfig.load(new File("config.yml"));
// Check connection
if (Redis.INSTANCE.isConnected()) {
    // ...
}
```

### Operations

```java
// Pub/Sub
Redis.INSTANCE.subscribe("my-channel", (channel, message) -> {
    System.out.println("Received: " + message);
});
Redis.INSTANCE.publish("my-channel","Hello!");
// Leaderboards (ZSET)
Redis.INSTANCE.zaddOne("kills", 100, "player-uuid");
List<String> topPlayers = Redis.INSTANCE.zrangeAll("kills");
```

## Building the Project

### Requirements

- **Java**: Java 21 or higher.
- **Gradle**: The project uses the Gradle wrapper (`./gradlew`), so no local installation is strictly required.

### Build Instructions

To build the project and generate the JAR files:

```bash
./gradlew build
```

The resulting JAR files (including the shadow/fat JAR) will be located in `build/libs/`.

## Contributing

Contributions are welcome! If you have any improvements, bug fixes, or new features to suggest, feel free to open a Pull
Request. We appreciate any help in making this project better.

## Coding Style

This project follows a strict coding style for robustness and clarity:

- **Annotations**: Every method parameter, return type, and field is annotated with `@NotNull` or `@Nullable` from
  JetBrains annotations.
- **Validation**: All `@NotNull` parameters are validated using `Objects.requireNonNull(param, "message")` at the
  beginning of the method.
