package qouteall.imm_ptl.core.mixin.common.container_gui;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.teleportation.ServerTeleportationManager;

@Mixin(RandomizableContainerBlockEntity.class)
public abstract class MixinRandomizableContainerBlockEntity extends BaseContainerBlockEntity {
    
    protected MixinRandomizableContainerBlockEntity(
        BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState
    ) {
        super(blockEntityType, blockPos, blockState);
        throw new RuntimeException();
    }
    
    // allow the player to open chest through portals
    @Inject(method = "stillValid", at = @At("RETURN"), cancellable = true)
    private void onStillValid(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (!cir.getReturnValue()) {
                if (level != null) {
                    boolean reachable = ServerTeleportationManager.canPlayerReachPos(
                        serverPlayer,
                        level.dimension(),
                        Vec3.atCenterOf(worldPosition)
                    );
                    cir.setReturnValue(reachable);
                }
            }
        }
        
    }
}
