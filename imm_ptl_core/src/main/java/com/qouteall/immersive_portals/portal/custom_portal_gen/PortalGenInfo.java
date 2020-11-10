package com.qouteall.immersive_portals.portal.custom_portal_gen;

import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalManipulation;
import com.qouteall.immersive_portals.portal.nether_portal.BlockPortalShape;
import com.qouteall.immersive_portals.portal.nether_portal.BreakablePortalEntity;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalGeneration;
import net.minecraft.entity.EntityType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

public class PortalGenInfo {
    public RegistryKey<World> from;
    public RegistryKey<World> to;
    public BlockPortalShape fromShape;
    public BlockPortalShape toShape;
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
        
        if (rotation != null) {
            if (Math.abs(1.0 - rotation.getW()) < 0.001) {
                this.rotation = null;
            }
        }
    }
    
    public <T extends Portal> T createTemplatePortal(EntityType<T> entityType) {
        ServerWorld fromWorld = McHelper.getServer().getWorld(from);
        
        T portal = entityType.create(fromWorld);
        fromShape.initPortalPosAxisShape(portal, false);
        portal.dimensionTo = to;
        portal.setDestination(toShape.innerAreaBox.getCenterVec());
        portal.scaling = scale;
        portal.rotation = rotation;
        
        if (portal.hasScaling() || portal.rotation != null) {
            portal.getExtension().adjustPositionAfterTeleport = true;
        }
        
        return portal;
    }
    
    public <T extends BreakablePortalEntity> T[] generateBiWayBiFacedPortal(
        EntityType<T> entityType
    ) {
        ServerWorld fromWorld = McHelper.getServer().getWorld(from);
        ServerWorld toWorld = McHelper.getServer().getWorld(to);
        
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
        
        return ((T[]) new BreakablePortalEntity[]{f1, f2, t1, t2});
    }
    
    public void generatePlaceholderBlocks() {
        MinecraftServer server = McHelper.getServer();
        
        NetherPortalGeneration.fillInPlaceHolderBlocks(
            server.getWorld(from), fromShape
        );
        NetherPortalGeneration.fillInPlaceHolderBlocks(
            server.getWorld(to), toShape
        );
    }
}
