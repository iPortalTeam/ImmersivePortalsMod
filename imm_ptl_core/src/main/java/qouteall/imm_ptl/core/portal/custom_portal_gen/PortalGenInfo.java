package qouteall.imm_ptl.core.portal.custom_portal_gen;

import net.minecraft.entity.EntityType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalExtension;
import qouteall.imm_ptl.core.portal.PortalManipulation;
import qouteall.imm_ptl.core.portal.nether_portal.BlockPortalShape;
import qouteall.imm_ptl.core.portal.nether_portal.BreakablePortalEntity;
import qouteall.imm_ptl.core.portal.nether_portal.NetherPortalGeneration;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.my_util.SignalArged;

import javax.annotation.Nullable;

public class PortalGenInfo {
    public static final SignalArged<PortalGenInfo> generatedSignal = new SignalArged<>();
    
    public RegistryKey<World> from;
    public RegistryKey<World> to;
    public BlockPortalShape fromShape;
    public BlockPortalShape toShape;
    @Nullable
    public Quaternion rotation = null;
    public double scale = 1.0;
    
    public PortalGenInfo(
        RegistryKey<World> from,
        RegistryKey<World> to,
        BlockPortalShape fromShape,
        BlockPortalShape toShape
    ) {
        this.from = from;
        this.to = to;
        this.fromShape = fromShape;
        this.toShape = toShape;
    }
    
    public PortalGenInfo(
        RegistryKey<World> from,
        RegistryKey<World> to,
        BlockPortalShape fromShape,
        BlockPortalShape toShape,
        Quaternion rotation,
        double scale
    ) {
        this.from = from;
        this.to = to;
        this.fromShape = fromShape;
        this.toShape = toShape;
        this.rotation = rotation;
        this.scale = scale;
        
        //floating point inaccuracy may make the portal to have near identity rotation or scale
        if (rotation != null) {
            if (Math.abs(1.0 - rotation.getW()) < 0.001) {
                this.rotation = null;
            }
        }
        
        if (Math.abs(this.scale - 1.0) < 0.00001) {
            this.scale = 1.0;
        }
    }
    
    public <T extends Portal> T createTemplatePortal(EntityType<T> entityType) {
        ServerWorld fromWorld = MiscHelper.getServer().getWorld(from);
        
        T portal = entityType.create(fromWorld);
        fromShape.initPortalPosAxisShape(portal, false);
        portal.dimensionTo = to;
        portal.setDestination(toShape.innerAreaBox.getCenterVec());
        portal.scaling = scale;
        portal.rotation = rotation;
        
        if (portal.hasScaling() || portal.rotation != null) {
            PortalExtension.get(portal).adjustPositionAfterTeleport = true;
        }
        
        return portal;
    }
    
    public <T extends BreakablePortalEntity> BreakablePortalEntity[] generateBiWayBiFacedPortal(
        EntityType<T> entityType
    ) {
        ServerWorld fromWorld = MiscHelper.getServer().getWorld(from);
        ServerWorld toWorld = MiscHelper.getServer().getWorld(to);
        
        T f1 = createTemplatePortal(entityType);
        
        T f2 = PortalManipulation.createFlippedPortal(f1, entityType);
        
        T t1 = PortalManipulation.createReversePortal(f1, entityType);
        T t2 = PortalManipulation.createFlippedPortal(t1, entityType);
        
        f1.blockPortalShape = fromShape;
        f2.blockPortalShape = fromShape;
        t1.blockPortalShape = toShape;
        t2.blockPortalShape = toShape;
        
        f1.reversePortalId = t1.getUuid();
        t1.reversePortalId = f1.getUuid();
        f2.reversePortalId = t2.getUuid();
        t2.reversePortalId = f2.getUuid();
        
        McHelper.spawnServerEntity(f1);
        McHelper.spawnServerEntity(f2);
        McHelper.spawnServerEntity(t1);
        McHelper.spawnServerEntity(t2);
        
        return ( new BreakablePortalEntity[]{f1, f2, t1, t2});
    }
    
    public void generatePlaceholderBlocks() {
        MinecraftServer server = MiscHelper.getServer();
        
        NetherPortalGeneration.fillInPlaceHolderBlocks(
            server.getWorld(from), fromShape
        );
        NetherPortalGeneration.fillInPlaceHolderBlocks(
            server.getWorld(to), toShape
        );
        
        generatedSignal.emit(this);
    }
}
