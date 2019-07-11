package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.exposer.IEPlayerListEntry;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PlayerListEntry.class)
public class MixinPlayerListEntry implements IEPlayerListEntry {
    @Shadow
    private GameMode gameMode;
    
    @Override
    public void setGameMode(GameMode mode) {
        gameMode = mode;
    }
}
