package qouteall.imm_ptl.core.mixin.client.sync;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import qouteall.imm_ptl.core.chunk_loading.MyClientChunkManager;

@Mixin(ReceivingLevelScreen.class)
public class MixinReceivingLevelScreen {
//    @Redirect(
//        method = "tick",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/client/renderer/LevelRenderer;isChunkCompiled(Lnet/minecraft/core/BlockPos;)Z"
//        )
//    )
//    private boolean redirectIsChunkCompiled(LevelRenderer instance, BlockPos blockPos) {
//        Minecraft client = Minecraft.getInstance();
//        ClientLevel world = client.level;
//
//        ClientChunkCache chunkSource = world.getChunkSource();
//
//        if (chunkSource instanceof MyClientChunkManager myClientChunkManager) {
//            ChunkPos chunkPos = new ChunkPos(blockPos);
//
//            return myClientChunkManager.isChunkLoaded(chunkPos.x, chunkPos.z);
//        }
//        else {
//            return instance.isChunkCompiled(blockPos);
//        }
//    }
}
