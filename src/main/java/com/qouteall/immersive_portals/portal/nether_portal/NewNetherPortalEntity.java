package com.qouteall.immersive_portals.portal.nether_portal;

import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.portal.IBreakablePortal;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalPlaceholderBlock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Random;
import java.util.UUID;

public class NewNetherPortalEntity extends Portal implements IBreakablePortal {
    public static EntityType<NewNetherPortalEntity> entityType;
    
    public NetherPortalShape netherPortalShape;
    public UUID reversePortalId;
    public boolean unbreakable = false;
    
    private boolean isNotified = true;
    private boolean shouldBreakNetherPortal = false;
    
    public NewNetherPortalEntity(
        EntityType<?> entityType_1,
        World world_1
    ) {
        super(entityType_1, world_1);
    }
    
    @Override
    public boolean isPortalValid() {
        if (world.isClient) {
            return super.isPortalValid();
        }
        return super.isPortalValid() && netherPortalShape != null && reversePortalId != null;
    }
    
    @Override
    protected void readCustomDataFromTag(CompoundTag compoundTag) {
        super.readCustomDataFromTag(compoundTag);
        if (compoundTag.contains("netherPortalShape")) {
            netherPortalShape = new NetherPortalShape(compoundTag.getCompound("netherPortalShape"));
        }
        reversePortalId = compoundTag.getUuid("reversePortalId");
        unbreakable = compoundTag.getBoolean("unbreakable");
    }
    
    @Override
    protected void writeCustomDataToTag(CompoundTag compoundTag) {
        super.writeCustomDataToTag(compoundTag);
        if (netherPortalShape != null) {
            compoundTag.put("netherPortalShape", netherPortalShape.toTag());
        }
        compoundTag.putUuid("reversePortalId", reversePortalId);
        compoundTag.putBoolean("unbreakable", unbreakable);
    }
    
    
    private void breakPortalOnThisSide() {
        assert shouldBreakNetherPortal;
        assert !removed;
        
        netherPortalShape.area.forEach(
            blockPos -> {
                if (world.getBlockState(blockPos).getBlock() == PortalPlaceholderBlock.instance) {
                    world.setBlockState(
                        blockPos, Blocks.AIR.getDefaultState()
                    );
                }
            }
        );
        this.remove();
        
        Helper.log("Broke " + this);
    }
    
    @Override
    public void notifyPlaceholderUpdate() {
        isNotified = true;
    }
    
    private NewNetherPortalEntity getReversePortal() {
        assert !world.isClient;
        
        ServerWorld world = getServer().getWorld(dimensionTo);
        return (NewNetherPortalEntity) world.getEntity(reversePortalId);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (world.isClient) {
            addSoundAndParticle();
        }
        else {
            if (!unbreakable) {
                if (isNotified) {
                    isNotified = false;
                    checkPortalIntegrity();
                }
                if (shouldBreakNetherPortal) {
                    breakPortalOnThisSide();
                }
            }
        }
        
    }
    
    private void checkPortalIntegrity() {
        assert !world.isClient;
        
        if (!isPortalValid()) {
            remove();
            return;
        }
        
        if (!isPortalIntactOnThisSide()) {
            shouldBreakNetherPortal = true;
            NewNetherPortalEntity reversePortal = getReversePortal();
            if (reversePortal != null) {
                reversePortal.shouldBreakNetherPortal = true;
            }
        }
    }
    
    private boolean isPortalIntactOnThisSide() {
        assert McHelper.getServer() != null;
        
        return netherPortalShape.area.stream()
            .allMatch(blockPos ->
                world.getBlockState(blockPos).getBlock() == PortalPlaceholderBlock.instance
            ) &&
            netherPortalShape.frameAreaWithoutCorner.stream()
                .allMatch(blockPos ->
                    O_O.isObsidian(world, blockPos)
                );
    }
    
    @Environment(EnvType.CLIENT)
    private void addSoundAndParticle() {
        Random random = world.getRandom();

        for (int i = 0; i < (int) Math.ceil(width * height / 20); i++) {
            if (random.nextInt(8) == 0) {
                double px = (random.nextDouble() * 2 - 1) * (width / 2);
                double py = (random.nextDouble() * 2 - 1) * (height / 2);
                
                Vec3d pos = getPointInPlane(px, py);
                
                double speedMultiplier = 20;
                
                double vx = speedMultiplier * ((double) random.nextFloat() - 0.5D) * 0.5D;
                double vy = speedMultiplier * ((double) random.nextFloat() - 0.5D) * 0.5D;
                double vz = speedMultiplier * ((double) random.nextFloat() - 0.5D) * 0.5D;
                
                world.addParticle(
                    ParticleTypes.PORTAL,
                    pos.x, pos.y, pos.z,
                    vx, vy, vz
                );
            }
        }
        
        if (random.nextInt(400) == 0) {
            world.playSound(
                getX(),
                getY(),
                getZ(),
                SoundEvents.BLOCK_PORTAL_AMBIENT,
                SoundCategory.BLOCKS,
                0.5F,
                random.nextFloat() * 0.4F + 0.8F,
                false
            );
        }
    }
    
}
