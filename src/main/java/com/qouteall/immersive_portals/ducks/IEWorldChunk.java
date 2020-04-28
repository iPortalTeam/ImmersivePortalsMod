package com.qouteall.immersive_portals.ducks;

import net.minecraft.entity.Entity;
import net.minecraft.util.TypeFilterableList;

public interface IEWorldChunk {
    TypeFilterableList<Entity>[] getEntitySections();
}
