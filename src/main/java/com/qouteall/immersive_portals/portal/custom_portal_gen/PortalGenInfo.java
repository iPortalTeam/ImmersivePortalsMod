package com.qouteall.immersive_portals.portal.custom_portal_gen;

import com.qouteall.immersive_portals.portal.nether_portal.BlockPortalShape;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

public class PortalGenInfo {
    public RegistryKey<World> from;
    public RegistryKey<World> to;
    public BlockPortalShape fromShape;
    public BlockPortalShape toShape;
    
    public PortalGenInfo(
        RegistryKey<World> from,
        RegistryKey<World> to,
        BlockPortalShape fromShape,
        BlockPortalShape toShape
    ) {
        this.from = from;
        this.to = to;
        this.fromShape = fromShape;
        this.toShape = toShape;
    }
}
