package qouteall.imm_ptl.core.compat.sodium_compatibility.mixin;

import me.jellysquid.mods.sodium.client.render.chunk.graph.ChunkGraphInfo;
import me.jellysquid.mods.sodium.client.util.math.FrustumExtended;
import me.jellysquid.mods.sodium.mixin.core.frustum.MixinFrustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import qouteall.imm_ptl.core.ducks.IEFrustum;

@Mixin(value = ChunkGraphInfo.class, remap = false)
public abstract class MixinSodiumChunkGraphInfo {
    @Shadow
    public abstract int getOriginX();
    
    @Shadow
    public abstract int getOriginY();
    
    @Shadow
    public abstract int getOriginZ();
    
    /**
     * @author qouteall
     * @reason ...
     */
    @Overwrite
    public boolean isCulledByFrustum(FrustumExtended frustum) {
        float x = this.getOriginX();
        float y = this.getOriginY();
        float z = this.getOriginZ();
        
        if (((IEFrustum) frustum).canDetermineInvisible(
            x, y, z, x + 16.0f, y + 16.0f, z + 16.0f
        )) {
            return true;
        }
        
        
        return !frustum.fastAabbTest(x, y, z, x + 16.0f, y + 16.0f, z + 16.0f);
    }
}
