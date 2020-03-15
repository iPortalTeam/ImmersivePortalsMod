package com.qouteall.immersive_portals.altius_world;

import net.minecraft.world.level.LevelGeneratorType;

public interface LevelGeneratorTypeConstructor {
    LevelGeneratorType construct(int id, String name, String storedName, int version);
}
