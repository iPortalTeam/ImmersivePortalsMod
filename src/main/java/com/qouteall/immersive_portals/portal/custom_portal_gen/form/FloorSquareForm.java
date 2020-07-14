package com.qouteall.immersive_portals.portal.custom_portal_gen.form;

import com.mojang.serialization.Codec;
import com.qouteall.immersive_portals.portal.custom_portal_gen.CustomPortalGeneration;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class FloorSquareForm extends PortalGenForm {
    @Override
    public Codec<PortalGenForm> getCodec() {
        return null;
    }
    
    @Override
    public PortalGenForm getReverse() {
        return null;
    }
    
    @Override
    public boolean perform(CustomPortalGeneration cpg, ServerWorld world, BlockPos startingPos) {
        return false;
    }
}
