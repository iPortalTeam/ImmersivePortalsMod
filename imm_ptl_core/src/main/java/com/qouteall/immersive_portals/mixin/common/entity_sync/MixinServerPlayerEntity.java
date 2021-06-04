package com.qouteall.immersive_portals.mixin.common.entity_sync;

import com.mojang.authlib.GameProfile;
import com.qouteall.immersive_portals.ducks.IEServerPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends PlayerEntity implements IEServerPlayerEntity {
    @Shadow
    public ServerPlayNetworkHandler networkHandler;
    @Shadow
    private Vec3d enteredNetherPos;
    
//    private HashMultimap<RegistryKey<World>, Entity> myRemovedEntities;
    
    @Shadow
    private boolean inTeleportationState;
    
    @Shadow protected abstract void worldChanged(ServerWorld origin);
    
    public MixinServerPlayerEntity(World world, BlockPos pos, float yaw, GameProfile profile) {
        super(world, pos, yaw, profile);
    }
    
//    @Inject(
//        method = "Lnet/minecraft/server/network/ServerPlayerEntity;tick()V",
//        at = @At("TAIL")
//    )
//    private void onTicking(CallbackInfo ci) {
//        if (myRemovedEntities != null) {
//            myRemovedEntities.keySet().forEach(dimension -> {
//                Set<Entity> list = myRemovedEntities.get(dimension);
//                networkHandler.sendPacket(
//                    MyNetwork.createRedirectedMessage(
//                        dimension,
//                        new EntityDestroyS2CPacket(
//                            list.stream().mapToInt(
//                                Entity::getId
//                            ).toArray()
//                        )
//                    )
//                );
//            });
//            myRemovedEntities = null;
//        }
//    }
    
//    @Inject(
//        method = "Lnet/minecraft/server/network/ServerPlayerEntity;copyFrom(Lnet/minecraft/server/network/ServerPlayerEntity;Z)V",
//        at = @At("RETURN")
//    )
//    private void onCopyFrom(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
//        HashMultimap<RegistryKey<World>, Entity> oldPlayerRemovedEntities =
//            ((MixinServerPlayerEntity) (Object) oldPlayer).myRemovedEntities;
//        if (oldPlayerRemovedEntities != null) {
//            myRemovedEntities = HashMultimap.create();
//            this.myRemovedEntities.putAll(oldPlayerRemovedEntities);
//        }
//    }
    
//    /**
//     * @author qouteall
//     */
//    @Overwrite
//    public void onStoppedTracking(Entity entity_1) {
//        if (entity_1 instanceof PlayerEntity) {
//            this.networkHandler.sendPacket(
//                MyNetwork.createRedirectedMessage(
//                    entity_1.world.getRegistryKey(),
//                    new EntityDestroyS2CPacket(entity_1.getId())
//                )
//            );
//        }
//        else {
//            if (myRemovedEntities == null) {
//                myRemovedEntities = HashMultimap.create();
//            }
//            //do not use entity.dimension
//            //or it will work abnormally when changeDimension() is run
//            myRemovedEntities.put(entity_1.world.getRegistryKey(), entity_1);
//        }
//
//    }
    
//    /**
//     * @author qouteall
//     */
//    @Overwrite
//    public void onStartedTracking(Entity entity_1) {
//        if (myRemovedEntities != null) {
//            myRemovedEntities.remove(entity_1.world.getRegistryKey(), entity_1);
//        }
//    }
    
    @Override
    public void setEnteredNetherPos(Vec3d pos) {
        enteredNetherPos = pos;
    }
    
    @Override
    public void updateDimensionTravelAdvancements(ServerWorld fromWorld) {
        worldChanged(fromWorld);
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
    
    @Override
    public void portal_worldChanged(ServerWorld fromWorld) {
        worldChanged(fromWorld);
    }
}
