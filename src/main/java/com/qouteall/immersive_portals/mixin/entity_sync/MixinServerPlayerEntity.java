package com.qouteall.immersive_portals.mixin.entity_sync;

import com.google.common.collect.HashMultimap;
import com.mojang.authlib.GameProfile;
import com.qouteall.immersive_portals.MyNetwork;
import com.qouteall.immersive_portals.SGlobal;
import com.qouteall.immersive_portals.ducks.IEServerPlayerEntity;
import net.minecraft.client.network.packet.EntitiesDestroyS2CPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends PlayerEntity implements IEServerPlayerEntity {
    @Shadow
    public ServerPlayNetworkHandler networkHandler;
    @Shadow
    private Vec3d enteredNetherPos;
    
    private HashMultimap<DimensionType, Entity> myRemovedEntities;
    
    public MixinServerPlayerEntity(
        World world,
        GameProfile profile
    ) {
        super(world, profile);
        throw new IllegalStateException();
    }
    
    @Shadow
    private boolean inTeleportationState;
    
    @Shadow
    protected abstract void dimensionChanged(ServerWorld targetWorld);
    
    @Inject(
        method = "Lnet/minecraft/server/network/ServerPlayerEntity;sendUnloadChunkPacket(Lnet/minecraft/util/math/ChunkPos;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSendUnloadChunkPacket(ChunkPos chunkPos_1, CallbackInfo ci) {
        ci.cancel();
    }
    
    @Inject(
        method = "Lnet/minecraft/server/network/ServerPlayerEntity;tick()V",
        at = @At("TAIL")
    )
    private void onTicking(CallbackInfo ci) {
        if (myRemovedEntities != null) {
            myRemovedEntities.keySet().forEach(dimension -> {
                Set<Entity> list = myRemovedEntities.get(dimension);
                networkHandler.sendPacket(
                    MyNetwork.createRedirectedMessage(
                        dimension,
                        new EntitiesDestroyS2CPacket(
                            list.stream().mapToInt(
                                Entity::getEntityId
                            ).toArray()
                        )
                    )
                );
            });
            myRemovedEntities = null;
        }
    }
    
    @Inject(method = "changeDimension", at = @At("HEAD"))
    private void onChangeDimensionByVanilla(
        DimensionType dimensionType_1,
        CallbackInfoReturnable<Entity> cir
    ) {
        SGlobal.chunkDataSyncManager.onPlayerRespawn((ServerPlayerEntity) (Object) this);
    }
    
    /**
     * @author qouteall
     */
    @Overwrite
    public void onStoppedTracking(Entity entity_1) {
        if (entity_1 instanceof PlayerEntity) {
            this.networkHandler.sendPacket(
                MyNetwork.createRedirectedMessage(
                    entity_1.dimension,
                    new EntitiesDestroyS2CPacket(entity_1.getEntityId())
                )
            );
        }
        else {
            if (myRemovedEntities == null) {
                myRemovedEntities = HashMultimap.create();
            }
            //do not use entity.dimension
            //or it will work abnormally when changeDimension() is run
            myRemovedEntities.put(entity_1.world.dimension.getType(), entity_1);
        }
        
    }
    
    /**
     * @author qouteall
     */
    @Overwrite
    public void onStartedTracking(Entity entity_1) {
        if (myRemovedEntities != null) {
            myRemovedEntities.remove(entity_1.dimension, entity_1);
        }
    }
    
    @Override
    public void setEnteredNetherPos(Vec3d pos) {
        enteredNetherPos = pos;
    }
    
    @Override
    public void updateDimensionTravelAdvancements(ServerWorld fromWorld) {
        dimensionChanged(fromWorld);
    }
    
    @Override
    public void setIsInTeleportationState(boolean arg) {
        inTeleportationState = arg;
    }
    
    @Override
    public void stopRidingWithoutTeleportRequest() {
        super.stopRiding();
    }
    
    @Override
    public void startRidingWithoutTeleportRequest(Entity newVehicle) {
        super.startRiding(newVehicle, true);
    }
}
