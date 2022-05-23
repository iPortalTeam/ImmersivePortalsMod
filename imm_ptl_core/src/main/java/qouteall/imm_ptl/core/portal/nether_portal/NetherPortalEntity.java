package qouteall.imm_ptl.core.portal.nether_portal;

import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.imm_ptl.core.portal.PortalPlaceholderBlock;

import java.util.Random;

public class NetherPortalEntity extends BreakablePortalEntity {
    public static EntityType<NetherPortalEntity> entityType;
    
    public NetherPortalEntity(
        EntityType<?> entityType_1,
        Level world_1
    ) {
        super(entityType_1, world_1);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (level.isClientSide()) {
            updateClientSideOverlayInfo();
            
        }
    }
    
    @Override
    protected boolean isPortalIntactOnThisSide() {
        
        return blockPortalShape.area.stream()
            .allMatch(blockPos ->
                level.getBlockState(blockPos).getBlock() == PortalPlaceholderBlock.instance
            ) &&
            blockPortalShape.frameAreaWithoutCorner.stream()
                .allMatch(blockPos ->
                    O_O.isObsidian(level.getBlockState(blockPos))
                );
    }
    
    @Override
    @Environment(EnvType.CLIENT)
    protected void addSoundAndParticle() {
        if (!IPGlobal.enableNetherPortalEffect) {
            return;
        }
        
        Random random = level.getRandom();
        
        for (int i = 0; i < (int) Math.ceil(width * height / 20); i++) {
            if (random.nextInt(10) == 0) {
                double px = (random.nextDouble() * 2 - 1) * (width / 2);
                double py = (random.nextDouble() * 2 - 1) * (height / 2);
                
                Vec3 pos = getPointInPlane(px, py);
                
                double speedMultiplier = 20;
                
                double vx = speedMultiplier * ((double) random.nextFloat() - 0.5D) * 0.5D;
                double vy = speedMultiplier * ((double) random.nextFloat() - 0.5D) * 0.5D;
                double vz = speedMultiplier * ((double) random.nextFloat() - 0.5D) * 0.5D;
                
                level.addParticle(
                    ParticleTypes.PORTAL,
                    pos.x, pos.y, pos.z,
                    vx, vy, vz
                );
            }
        }
        
        if (random.nextInt(800) == 0) {
            level.playLocalSound(
                getX(),
                getY(),
                getZ(),
                SoundEvents.PORTAL_AMBIENT,
                SoundSource.BLOCKS,
                0.5F,
                random.nextFloat() * 0.4F + 0.8F,
                false
            );
        }
    }
    
    void updateClientSideOverlayInfo() {
        if (!IPGlobal.netherPortalOverlay) {
            overlayInfo = null;
            return;
        }

//        if (overlayInfo != null) {
//            // avoid repeating update every tick
//            return;
//        }
        
        Direction.Axis axis = blockPortalShape.axis;
        
        switch (axis) {
            case X -> {
                overlayInfo = new OverlayInfo(
                    Blocks.NETHER_PORTAL.defaultBlockState().setValue(
                        NetherPortalBlock.AXIS,
                        Direction.Axis.Z
                    ),
                    0.5,
                    0,
                    null
                );
            }
            case Y -> {
                double offset = getNormal().y > 0 ? 1 : -1;
                overlayInfo = new OverlayInfo(
                    Blocks.NETHER_PORTAL.defaultBlockState().setValue(
                        NetherPortalBlock.AXIS,
                        Direction.Axis.X
                    ),
                    0.5,
                    offset,
                    new Quaternion(new Vector3f(1, 0, 0), 90, true)
                );
            }
            case Z -> {
                overlayInfo = new OverlayInfo(
                    Blocks.NETHER_PORTAL.defaultBlockState().setValue(
                        NetherPortalBlock.AXIS,
                        Direction.Axis.X
                    ),
                    0.5,
                    0,
                    null
                );
            }
        }
    }
}
