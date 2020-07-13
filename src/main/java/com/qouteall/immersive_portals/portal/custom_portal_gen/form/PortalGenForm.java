package com.qouteall.immersive_portals.portal.custom_portal_gen.form;

import com.mojang.serialization.Codec;
import com.qouteall.immersive_portals.portal.custom_portal_gen.CustomPortalGeneration;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public abstract class PortalGenForm {
    public static Codec<PortalGenForm> codec;
    
    public abstract Codec<PortalGenForm> getCodec();
    
    public abstract PortalGenForm getReverse();
    
    // Return true for succeeded
    public abstract boolean perform(
        CustomPortalGeneration cpg, ServerWorld world, BlockPos startingPos
    );
}
