package qouteall.imm_ptl.core.portal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import qouteall.imm_ptl.core.ducks.IEWorld;
import qouteall.imm_ptl.core.teleportation.ServerTeleportationManager;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Consumer;

// the additional features of a portal
public class PortalExtension {
    
    /**
     * @param portal
     * @return the portal extension object
     */
    public static PortalExtension get(Portal portal) {
        if (portal.extension == null) {
            portal.extension = new PortalExtension();
        }
        return portal.extension;
    }
    
    public static void init() {
        Portal.clientPortalTickSignal.connect(portal -> {
            get(portal).tick(portal);
            
        });
        
        Portal.serverPortalTickSignal.connect(portal -> {
            get(portal).tick(portal);
            
        });
        
        Portal.readPortalDataSignal.connect((portal, tag) -> {
            get(portal).readFromNbt(tag);
        });
        
        Portal.writePortalDataSignal.connect((portal, tag) -> {
            get(portal).writeToNbt(tag);
        });
    }
    
    /**
     * If positive, the player that's touching the portal will be accelerated
     * If negative, the player that's touching the portal and moving quickly will
     * be decelerated
     */
    public double motionAffinity = 0;
    
    /**
     * If true, when the player comes out from the portal and get stuck in block
     * the player will be smoothly levitated to avoid falling through floor
     */
    public boolean adjustPositionAfterTeleport = true;
    
    public boolean bindCluster = false;
    
    // these are stored in data
    @Nullable
    public UUID reversePortalId;
    @Nullable
    public UUID flippedPortalId;
    @Nullable
    public UUID parallelPortalId;
    
    // these are initialized at runtime
    @Nullable
    public Portal reversePortal;
    @Nullable
    public Portal flippedPortal;
    @Nullable
    public Portal parallelPortal;
    
    public PortalExtension() {
    
    }
    
    private void readFromNbt(CompoundTag compoundTag) {
        if (compoundTag.contains("motionAffinity")) {
            motionAffinity = compoundTag.getDouble("motionAffinity");
        }
        else {
            motionAffinity = 0;
        }
        if (compoundTag.contains("adjustPositionAfterTeleport")) {
            adjustPositionAfterTeleport = compoundTag.getBoolean("adjustPositionAfterTeleport");
        }
        else {
            adjustPositionAfterTeleport = true;
        }
        
        if (compoundTag.contains("bindCluster")) {
            bindCluster = compoundTag.getBoolean("bindCluster");
        }
        else {
            bindCluster = false;
        }
        
        if (compoundTag.contains("reversePortalId")) {
            reversePortalId = compoundTag.getUUID("reversePortalId");
        }
        else {
            reversePortalId = null;
        }
        if (compoundTag.contains("flippedPortalId")) {
            flippedPortalId = compoundTag.getUUID("flippedPortalId");
        }
        else {
            flippedPortalId = null;
        }
        if (compoundTag.contains("parallelPortalId")) {
            parallelPortalId = compoundTag.getUUID("parallelPortalId");
        }
        else {
            parallelPortalId = null;
        }
    }
    
    private void writeToNbt(CompoundTag compoundTag) {
        if (motionAffinity != 0) {
            compoundTag.putDouble("motionAffinity", motionAffinity);
        }
        compoundTag.putBoolean("adjustPositionAfterTeleport", adjustPositionAfterTeleport);
        compoundTag.putBoolean("bindCluster", bindCluster);
        if (reversePortalId != null) {
            compoundTag.putUUID("reversePortalId", reversePortalId);
        }
        if (flippedPortalId != null) {
            compoundTag.putUUID("flippedPortalId", flippedPortalId);
        }
        if (parallelPortalId != null) {
            compoundTag.putUUID("parallelPortalId", parallelPortalId);
        }
    }
    
    private void tick(Portal portal) {
        if (portal.level.isClientSide()) {
            updateClusterStatusClient(portal);
        }
        else {
            updateClusterStatusServer(portal);
        }
    }
    
    private void updateClusterStatusServer(Portal portal) {
        boolean needsUpdate = false;
        
        if (bindCluster) {
            if (flippedPortal != null) {
                if (flippedPortal.isRemoved()) {
                    flippedPortal = null;
                }
            }
            if (reversePortal != null) {
                if (reversePortal.isRemoved()) {
                    reversePortal = null;
                }
            }
            if (parallelPortal != null) {
                if (parallelPortal.isRemoved()) {
                    parallelPortal = null;
                }
            }
            
            if (flippedPortalId != null) {
                // if the id is not null, find the portal entity
                if (flippedPortal == null) {
                    Entity e = ((IEWorld) portal.level).portal_getEntityLookup().get(flippedPortalId);
                    if (e instanceof Portal p) {
                        flippedPortal = p;
                    }
                    else {
                        flippedPortalId = null;
                        needsUpdate = true;
                    }
                }
            }
            if (flippedPortalId == null) {
                // if the id is null, find the portal from world
                flippedPortal = PortalManipulation.findFlippedPortal(portal);
                if (flippedPortal != null) {
                    flippedPortalId = flippedPortal.getUUID();
                    needsUpdate = true;
                }
            }
            
            if (reversePortalId != null) {
                if (reversePortal == null) {
                    Entity e = ((IEWorld) portal.getDestWorld()).portal_getEntityLookup().get(reversePortalId);
                    if (e instanceof Portal p) {
                        reversePortal = p;
                    }
                    else {
                        if (portal.isOtherSideChunkLoaded()) {
                            reversePortalId = null;
                            needsUpdate = true;
                        }
                    }
                }
            }
            if (reversePortalId == null) {
                reversePortal = PortalManipulation.findReversePortal(portal);
                if (reversePortal != null) {
                    reversePortalId = reversePortal.getUUID();
                    needsUpdate = true;
                }
            }
            
            if (parallelPortalId != null) {
                if (parallelPortal == null) {
                    Entity e = ((IEWorld) portal.getDestWorld()).portal_getEntityLookup().get(parallelPortalId);
                    if (e instanceof Portal p) {
                        parallelPortal = p;
                    }
                    else {
                        if (portal.isOtherSideChunkLoaded()) {
                            parallelPortalId = null;
                            needsUpdate = true;
                        }
                    }
                }
            }
            if (parallelPortalId == null) {
                parallelPortal = PortalManipulation.findParallelPortal(portal);
                if (parallelPortal != null) {
                    parallelPortalId = parallelPortal.getUUID();
                    needsUpdate = true;
                }
            }
        }
        else {
            flippedPortal = null;
            reversePortal = null;
            parallelPortal = null;
            flippedPortalId = null;
            reversePortalId = null;
            parallelPortalId = null;
        }
        
        if (flippedPortal != null) {
            PortalExtension.get(flippedPortal).bindCluster = true;
        }
        if (reversePortal != null) {
            PortalExtension.get(reversePortal).bindCluster = true;
        }
        if (parallelPortal != null) {
            PortalExtension.get(parallelPortal).bindCluster = true;
        }
        
        if (needsUpdate) {
            portal.reloadAndSyncToClient();
        }
    }
    
    
    private void updateClusterStatusClient(Portal portal) {
        if (bindCluster) {
            if (flippedPortalId != null) {
                // if the id is not null, find the portal
                Entity e = ((IEWorld) portal.level).portal_getEntityLookup().get(flippedPortalId);
                if (e instanceof Portal p) {
                    flippedPortal = p;
                }
            }
            else {
                flippedPortal = null;
            }
            
            if (reversePortalId != null) {
                Entity e = ((IEWorld) portal.getDestWorld()).portal_getEntityLookup().get(reversePortalId);
                if (e instanceof Portal p) {
                    reversePortal = p;
                }
            }
            else {
                reversePortal = null;
            }
            
            if (parallelPortalId != null) {
                Entity e = ((IEWorld) portal.getDestWorld()).portal_getEntityLookup().get(parallelPortalId);
                if (e instanceof Portal p) {
                    parallelPortal = p;
                }
            }
            else {
                parallelPortal = null;
            }
        }
        else {
            flippedPortal = null;
            reversePortal = null;
            parallelPortal = null;
        }
    }
    
    // works on both client and server
    public void rectifyClusterPortals(Portal portal, boolean sync) {
        
        portal.animation.defaultAnimation.inverseScale = false;
        
        if (flippedPortal != null) {
            flippedPortal = ServerTeleportationManager.teleportRegularEntityTo(
                flippedPortal,
                portal.level.dimension(),
                portal.getOriginPos()
            );
            
            flippedPortal.dimensionTo = portal.dimensionTo;
            flippedPortal.setOriginPos(portal.getOriginPos());
            flippedPortal.setDestination(portal.getDestPos());
            
            flippedPortal.axisW = portal.axisW;
            flippedPortal.axisH = portal.axisH.scale(-1);
            
            flippedPortal.scaling = portal.scaling;
            flippedPortal.rotation = portal.rotation;
            
            if (flippedPortal.specialShape == null) {
                flippedPortal.width = portal.width;
                flippedPortal.height = portal.height;
            }
            
            // it will copy animation
            PortalManipulation.copyAdditionalProperties(flippedPortal, portal, false);
            
            flippedPortal.animation.defaultAnimation.inverseScale = false;
            
            if (sync) {
                flippedPortal.reloadAndSyncToClient();
            }
        }
        
        if (reversePortal != null) {
            reversePortal = ServerTeleportationManager.teleportRegularEntityTo(
                reversePortal,
                portal.getDestDim(),
                portal.getDestPos()
            );
            reversePortalId = reversePortal.getUUID();
            
            reversePortal.dimensionTo = portal.getOriginDim();
            reversePortal.setOriginPos(portal.getDestPos());
            reversePortal.setDestination(portal.getOriginPos());
            
            reversePortal.axisW = portal.transformLocalVecNonScale(portal.axisW);
            reversePortal.axisH = portal.transformLocalVecNonScale(portal.axisH.scale(-1));
            reversePortal.scaling = 1.0 / portal.scaling;
            if (portal.rotation != null) {
                reversePortal.rotation = portal.rotation.copy();
                reversePortal.rotation.conj();
            }
            else {
                reversePortal.rotation = null;
            }
            
            if (reversePortal.specialShape == null) {
                reversePortal.width = portal.width * portal.getScale();
                reversePortal.height = portal.height * portal.getScale();
            }
            
            // it will copy animation
            PortalManipulation.copyAdditionalProperties(reversePortal, portal, false);
            
            reversePortal.animation.defaultAnimation.inverseScale = true;
            
            if (sync) {
                reversePortal.reloadAndSyncToClient();
            }
        }
        
        if (parallelPortal != null) {
            parallelPortal = ServerTeleportationManager.teleportRegularEntityTo(
                parallelPortal,
                portal.getDestDim(),
                portal.getDestPos()
            );
            parallelPortalId = parallelPortal.getUUID();
            
            parallelPortal.dimensionTo = portal.getOriginDim();
            parallelPortal.setOriginPos(portal.getDestPos());
            parallelPortal.setDestination(portal.getOriginPos());
            
            parallelPortal.axisW = portal.transformLocalVecNonScale(portal.axisW);
            parallelPortal.axisH = portal.transformLocalVecNonScale(portal.axisH);
            parallelPortal.scaling = 1.0 / portal.scaling;
            if (portal.rotation != null) {
                parallelPortal.rotation = portal.rotation.copy();
                parallelPortal.rotation.conj();
            }
            else {
                parallelPortal.rotation = null;
            }
            
            if (parallelPortal.specialShape == null) {
                parallelPortal.width = portal.width * portal.getScale();
                parallelPortal.height = portal.height * portal.getScale();
            }
            
            // it will copy animation
            PortalManipulation.copyAdditionalProperties(parallelPortal, portal, false);
            
            parallelPortal.animation.defaultAnimation.inverseScale = true;
            
            if (sync) {
                parallelPortal.reloadAndSyncToClient();
            }
        }
    }
    
    // f1's reverse is t1
    public static void initializeClusterBind(
        Portal f1, Portal f2,
        Portal t1, Portal t2
    ) {
        get(f1).bindCluster = true;
        get(f2).bindCluster = true;
        get(t1).bindCluster = true;
        get(t2).bindCluster = true;
        
        get(f1).flippedPortalId = f2.getUUID();
        get(f2).flippedPortalId = f1.getUUID();
        
        get(t1).flippedPortalId = t2.getUUID();
        get(t2).flippedPortalId = t1.getUUID();
        
        get(f1).reversePortalId = t1.getUUID();
        get(t1).reversePortalId = f1.getUUID();
        
        get(f2).reversePortalId = t2.getUUID();
        get(t2).reversePortalId = f2.getUUID();
        
        get(f1).parallelPortalId = t2.getUUID();
        get(t2).parallelPortalId = f1.getUUID();
        
        get(f2).parallelPortalId = t1.getUUID();
        get(t1).parallelPortalId = f2.getUUID();
        
    }
    
    public static void forClusterPortals(Portal portal, Consumer<Portal> func) {
        func.accept(portal);
        
        forConnectedPortals(portal, func);
    }
    
    public static void forConnectedPortals(Portal portal, Consumer<Portal> func) {
        PortalExtension extension = PortalExtension.get(portal);
        if (extension.flippedPortal != null) {
            func.accept(extension.flippedPortal);
        }
        if (extension.reversePortal != null) {
            func.accept(extension.reversePortal);
        }
        if (extension.parallelPortal != null) {
            func.accept(extension.parallelPortal);
        }
    }
    
}
