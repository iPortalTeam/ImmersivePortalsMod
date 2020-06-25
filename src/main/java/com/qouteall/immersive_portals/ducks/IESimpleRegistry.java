package com.qouteall.immersive_portals.ducks;

import net.minecraft.util.registry.RegistryKey;

public interface IESimpleRegistry {
    void markUnloaded(RegistryKey<?> key);
}
