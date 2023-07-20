package qouteall.imm_ptl.core.portal.custom_portal_gen;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalExtension;
import qouteall.imm_ptl.core.portal.PortalManipulation;
import qouteall.imm_ptl.core.portal.nether_portal.BlockPortalShape;
import qouteall.imm_ptl.core.portal.nether_portal.BreakablePortalEntity;
import qouteall.imm_ptl.core.portal.nether_portal.NetherPortalGeneration;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.my_util.DQuaternion;
import qouteall.q_misc_util.my_util.SignalArged;

import org.jetbrains.annotations.Nullable;

public class PortalGenInfo {
    public static final SignalArged<PortalGenInfo> generatedSignal = new SignalArged<>();
    
    public ResourceKey<Level> from;
    public ResourceKey<Level> to;
    public BlockPortalShape fromShape;
    public BlockPortalShape toShape;
    @Nullable
    public DQuaternion rotation = null;
    public double scale = 1.0;
    
    public PortalGenInfo(
        ResourceKey<Level> from,
        ResourceKey<Level> to,
        BlockPortalShape fromShape,
        BlockPortalShape toShape
    ) {
        this.from = from;
        this.to = to;
        this.fromShape = fromShape;
        this.toShape = toShape;
    }
    
    public PortalGenInfo(
        ResourceKey<Level> from,
        ResourceKey<Level> to,
        BlockPortalShape fromShape,
        BlockPortalShape toShape,
        DQuaternion rotation,
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
            if (rotation.getRotatingAngleDegrees() < 0.001) {
                this.rotation = null;
            }
        }
        
        if (Math.abs(this.scale - 1.0) < 0.00001) {
            this.scale = 1.0;
        }
    }
    
    public <T extends Portal> T createTemplatePortal(EntityType<T> entityType) {
        ServerLevel fromWorld = McHelper.getServerWorld(from);
        
        T portal = entityType.create(fromWorld);
        assert portal != null;
        fromShape.initPortalPosAxisShape(portal, Direction.AxisDirection.POSITIVE);
        portal.dimensionTo = to;
        portal.setDestination(toShape.innerAreaBox.getCenterVec());
        portal.scaling = scale;
        portal.setRotation(rotation);
        
        return portal;
    }
    
    public <T extends BreakablePortalEntity> BreakablePortalEntity[] generateBiWayBiFacedPortal(
        EntityType<T> entityType
    ) {
        T f1 = createTemplatePortal(entityType);
        
        T f2 = PortalManipulation.createFlippedPortal(f1, entityType);
        
        T t1 = PortalManipulation.createReversePortal(f1, entityType);
        T t2 = PortalManipulation.createFlippedPortal(t1, entityType);
        
        f1.blockPortalShape = fromShape;
        f2.blockPortalShape = fromShape;
        t1.blockPortalShape = toShape;
        t2.blockPortalShape = toShape;
        
        f1.reversePortalId = t1.getUUID();
        t1.reversePortalId = f1.getUUID();
        f2.reversePortalId = t2.getUUID();
        t2.reversePortalId = f2.getUUID();
        
        PortalExtension.initializeClusterBind(f1, f2, t1, t2);
        
        McHelper.spawnServerEntity(f1);
        McHelper.spawnServerEntity(f2);
        McHelper.spawnServerEntity(t1);
        McHelper.spawnServerEntity(t2);
        
        return (new BreakablePortalEntity[]{f1, f2, t1, t2});
    }
    
    public void generatePlaceholderBlocks() {
        MinecraftServer server = MiscHelper.getServer();
        
        NetherPortalGeneration.fillInPlaceHolderBlocks(
            server.getLevel(from), fromShape
        );
        NetherPortalGeneration.fillInPlaceHolderBlocks(
            server.getLevel(to), toShape
        );
        
        generatedSignal.emit(this);
    }
}
