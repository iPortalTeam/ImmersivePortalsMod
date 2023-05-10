package qouteall.imm_ptl.core.compat.mixin;

import it.unimi.dsi.fastutil.objects.ObjectList;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.commons.lang3.Validate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.compat.sodium_compatibility.IESodiumRenderSectionManager;
import qouteall.imm_ptl.core.compat.sodium_compatibility.SodiumRenderingContext;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
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
    
    @Shadow
    @Final
    @Mutable
    private int renderDistance;
    
    @Override
    public void ip_swapContext(SodiumRenderingContext context) {
        Validate.isTrue(context.renderDistance != 0, "Render distance cannot be 0");
        
        ChunkRenderList chunkRenderListTmp = chunkRenderList;
        chunkRenderList = context.chunkRenderList;
        context.chunkRenderList = chunkRenderListTmp;
        
        ObjectList<RenderSection> tickableChunksTmp = tickableChunks;
        tickableChunks = context.tickableChunks;
        context.tickableChunks = tickableChunksTmp;
        
        ObjectList<BlockEntity> visibleBlockEntitiesTmp = visibleBlockEntities;
        visibleBlockEntities = context.visibleBlockEntities;
        context.visibleBlockEntities = visibleBlockEntitiesTmp;
        
        int renderDistanceTmp = renderDistance;
        renderDistance = context.renderDistance;
        context.renderDistance = renderDistanceTmp;
    }
    
    /**
     * The section visibility information will be wrong if rendered a portal.
     * Just cancel this optimization.
     * isSectionVisible() is currently only used for culling entities.
     */
    @Inject(method = "isSectionVisible", at = @At("HEAD"), cancellable = true)
    private void onIsSectionVisible(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        if (RenderStates.portalsRenderedThisFrame != 0) {
            cir.setReturnValue(true);
        }
    }
    
}
