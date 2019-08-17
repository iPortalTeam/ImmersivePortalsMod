package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.MyNetwork;
import com.qouteall.immersive_portals.SGlobal;
import com.qouteall.immersive_portals.chunk_loading.DimensionalChunkPos;
import com.qouteall.immersive_portals.exposer.IEServerPlayerEntity;
import net.minecraft.client.network.packet.EntitiesDestroyS2CPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.stream.Collectors;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity implements IEServerPlayerEntity {
    @Shadow
    public ServerPlayNetworkHandler networkHandler;
    @Shadow
    private Vec3d enteredNetherPos;
    
    private ArrayDeque<Entity> myRemovedEntities;
    
    @Shadow
    public abstract void method_18783(ServerWorld serverWorld_1);
    
    @Shadow
    private boolean inTeleportationState;
    
    @Override
    public void setEnteredNetherPos(Vec3d pos) {
        enteredNetherPos = pos;
    }
    
    @Override
    public void updateDimensionTravelAdvancements(ServerWorld fromWorld) {
        method_18783(fromWorld);
    }
    
    @Inject(
        method = "Lnet/minecraft/server/network/ServerPlayerEntity;sendUnloadChunkPacket(Lnet/minecraft/util/math/ChunkPos;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSendUnloadChunkPacket(ChunkPos chunkPos_1, CallbackInfo ci) {
        ServerPlayerEntity this_ = (ServerPlayerEntity) (Object) this;
        DimensionalChunkPos dimensionalChunkPos = new DimensionalChunkPos(
            this_.dimension,
            chunkPos_1
        );
    
        SGlobal.chunkDataSyncManager.sendUnloadPacket(
            this_, dimensionalChunkPos
        );
        
        ci.cancel();
    }
    
    @Inject(
        method = "Lnet/minecraft/server/network/ServerPlayerEntity;tick()V",
        at = @At("TAIL")
    )
    private void onTicking(CallbackInfo ci) {
        if (myRemovedEntities != null) {
            myRemovedEntities.stream()
                .collect(Collectors.groupingBy(entity -> entity.dimension))
                .forEach((dimension, list) -> networkHandler.sendPacket(
                    MyNetwork.createRedirectedMessage(
                        dimension,
                        new EntitiesDestroyS2CPacket(
                            list.stream().mapToInt(
                                Entity::getEntityId
                            ).toArray()
                        )
                    )
                ));
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
                myRemovedEntities = new ArrayDeque<>();
            }
            myRemovedEntities.add(entity_1);
        }
        
    }
    
    /**
     * @author qouteall
     */
    @Overwrite
    public void onStartedTracking(Entity entity_1) {
        if (myRemovedEntities != null) {
            myRemovedEntities.remove(entity_1);
        }
    }
    
    @Override
    public void setIsInTeleportationState(boolean arg) {
        inTeleportationState = arg;
    }
}
