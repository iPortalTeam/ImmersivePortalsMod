package qouteall.imm_ptl.core.compat.sodium_compatibility.mixin;

import it.unimi.dsi.fastutil.objects.ObjectList;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import net.minecraft.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.compat.sodium_compatibility.IESodiumRenderSectionManager;
import qouteall.imm_ptl.core.compat.sodium_compatibility.SodiumRenderingContext;
import qouteall.imm_ptl.core.render.context_management.RenderStates;

@Mixin(value = RenderSectionManager.class, remap = false)
public class MixinSodiumRenderSectionManager implements IESodiumRenderSectionManager {
    @Shadow
    @Final
    @Mutable
    private ChunkRenderList chunkRenderList;
    
    @Shadow
    @Final
    @Mutable
    private ObjectList<RenderSection> tickableChunks;
    
    @Shadow
    @Final
    @Mutable
    private ObjectList<BlockEntity> visibleBlockEntities;
    
    @Override
    public void ip_swapContext(SodiumRenderingContext context) {
        ChunkRenderList chunkRenderListTmp = chunkRenderList;
        chunkRenderList = context.chunkRenderList;
        context.chunkRenderList = chunkRenderListTmp;
        
        ObjectList<RenderSection> tickableChunksTmp = tickableChunks;
        tickableChunks = context.tickableChunks;
        context.tickableChunks = tickableChunksTmp;
        
        ObjectList<BlockEntity> visibleBlockEntitiesTmp = visibleBlockEntities;
        visibleBlockEntities = context.visibleBlockEntities;
        context.visibleBlockEntities = visibleBlockEntitiesTmp;
    }
    
    @Inject(method = "isSectionVisible", at = @At("HEAD"), cancellable = true)
    private void onIsSectionVisible(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        if (RenderStates.portalsRenderedThisFrame != 0) {
            // the section visibility information will be wrong if rendered a portal
            // just cancel this optimization
            cir.setReturnValue(true);
        }
    }
}
