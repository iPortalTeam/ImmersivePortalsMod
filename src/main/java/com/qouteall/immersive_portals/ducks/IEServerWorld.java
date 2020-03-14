package com.qouteall.immersive_portals.ducks;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;

import java.util.List;
import java.util.function.Predicate;

public interface IEServerWorld {
    public <T extends Entity> List<T> getEntitiesWithoutImmediateChunkLoading(
        Class<? extends T> entityClass,
        Box box,
        Predicate<? super T> predicate
    );
}
