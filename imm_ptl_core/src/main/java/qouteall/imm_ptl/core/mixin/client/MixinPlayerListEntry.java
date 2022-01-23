package qouteall.imm_ptl.core.mixin.client;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import qouteall.imm_ptl.core.ducks.IEPlayerListEntry;

@Mixin(PlayerInfo.class)
public class MixinPlayerListEntry implements IEPlayerListEntry {
    @Shadow
    private GameType gameMode;
    
    @Override
    public void setGameMode(GameType mode) {
        gameMode = mode;
    }
}
