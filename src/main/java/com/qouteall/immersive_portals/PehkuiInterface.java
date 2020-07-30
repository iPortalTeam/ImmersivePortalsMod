package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.entity.Entity;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PehkuiInterface {
    
    public static boolean isPehkuiPresent = false;
    
    public static Consumer<Portal> onClientPlayerTeleported = portal -> {
    };
    
    public static BiConsumer<Entity, Portal> onServerEntityTeleported = (e, p) -> {
    
    };
}
