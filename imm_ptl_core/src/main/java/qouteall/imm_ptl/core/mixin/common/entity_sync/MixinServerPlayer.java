package qouteall.imm_ptl.core.mixin.common.entity_sync;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import qouteall.imm_ptl.core.ducks.IEServerPlayerEntity;

@Mixin(ServerPlayer.class)
public abstract class MixinServerPlayer extends Player implements IEServerPlayerEntity {
    @Shadow
    public ServerGamePacketListenerImpl connection;
    @Shadow
    private Vec3 enteredNetherPosition;
    
    @Shadow
    private boolean isChangingDimension;
    
    public MixinServerPlayer(Level level, BlockPos blockPos, float f, GameProfile gameProfile) {
        super(level, blockPos, f, gameProfile);
    }
    
    @Shadow protected abstract void triggerDimensionChangeTriggers(ServerLevel origin);
    
    @Override
    public void setEnteredNetherPos(Vec3 pos) {
        enteredNetherPosition = pos;
    }
    
    @Override
    public void setIsInTeleportationState(boolean arg) {
        isChangingDimension = arg;
    }
    
    @Override
    public void stopRidingWithoutTeleportRequest() {
        super.stopRiding();
    }
    
    @Override
    public void startRidingWithoutTeleportRequest(Entity newVehicle) {
        super.startRiding(newVehicle, true);
    }
    
    /**
     * See {@link ServerPlayer#changeDimension(ServerLevel)}
     */
    @Override
    public void portal_worldChanged(ServerLevel fromWorld, Vec3 fromPos) {
        if (fromWorld.dimension() == Level.OVERWORLD && this.level().dimension() == Level.NETHER) {
            enteredNetherPosition = fromPos;
        }
        triggerDimensionChangeTriggers(fromWorld);
    }
}
