package com.craftlyworks.configra.mongo;

import org.bson.Document;
import org.jetbrains.annotations.NotNull;

public interface IMongoSerializable {
    @NotNull Document serialize();
}
