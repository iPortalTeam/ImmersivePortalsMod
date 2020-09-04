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
    }
    
    public <T extends Portal> T createTemplatePortal(EntityType<T> entityType) {
        ServerWorld fromWorld = McHelper.getServer().getWorld(from);
        
        T portal = entityType.create(fromWorld);
        fromShape.initPortalPosAxisShape(portal, false);
        portal.dimensionTo = to;
        portal.destination = toShape.innerAreaBox.getCenterVec();
        portal.scaling = scale;
        portal.rotation = rotation;
        
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
        
        fromWorld.spawnEntity(f1);
        fromWorld.spawnEntity(f2);
        toWorld.spawnEntity(t1);
        toWorld.spawnEntity(t2);
        
        return ((T[]) new BreakablePortalEntity[]{f1, f2, t1, t2});
    }
    
    public void generatePlaceholderBlocks(){
        MinecraftServer server = McHelper.getServer();
        
        NetherPortalGeneration.fillInPlaceHolderBlocks(
            server.getWorld(from), fromShape
        );
        NetherPortalGeneration.fillInPlaceHolderBlocks(
            server.getWorld(to), toShape
        );
    }
}
