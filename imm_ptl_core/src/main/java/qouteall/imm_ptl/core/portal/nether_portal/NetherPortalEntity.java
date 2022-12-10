package qouteall.imm_ptl.core.portal.nether_portal;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.imm_ptl.core.portal.PortalPlaceholderBlock;
import qouteall.q_misc_util.my_util.DQuaternion;

public class NetherPortalEntity extends BreakablePortalEntity {
    private static final OverlayInfo overlay_x = new OverlayInfo(
        Blocks.NETHER_PORTAL.defaultBlockState().setValue(
            NetherPortalBlock.AXIS,
            Direction.Axis.Z
        ),
        0.5,
        0,
        null
    );
    private static final OverlayInfo overlay_y_up = new OverlayInfo(
        Blocks.NETHER_PORTAL.defaultBlockState().setValue(
            NetherPortalBlock.AXIS,
            Direction.Axis.X
        ),
        0.5,
        1,
        DQuaternion.rotationByDegrees(new Vec3(1, 0, 0), 90)
    );
    private static final OverlayInfo overlay_y_down = new OverlayInfo(
        Blocks.NETHER_PORTAL.defaultBlockState().setValue(
            NetherPortalBlock.AXIS,
            Direction.Axis.X
        ),
        0.5,
        -1,
        DQuaternion.rotationByDegrees(new Vec3(1, 0, 0), 90)
    );
    private static final OverlayInfo overlay_z = new OverlayInfo(
        Blocks.NETHER_PORTAL.defaultBlockState().setValue(
            NetherPortalBlock.AXIS,
            Direction.Axis.X
        ),
        0.5,
        0,
        null
    );
    
    
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
        
        RandomSource random = level.getRandom();
        
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
    
    @Override
    public OverlayInfo getActualOverlay() {
        if (IPGlobal.netherPortalOverlay) {
            switch (blockPortalShape.axis) {
                case X -> {return overlay_x;}
                case Y -> {return getNormal().y > 0 ? overlay_y_up : overlay_y_down;}
                case Z -> {return overlay_z;}
            }
        }
        
        return super.getActualOverlay();
    }
}
