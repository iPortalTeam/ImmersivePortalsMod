package qouteall.imm_ptl.core.portal;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.NbtCompound;
import qouteall.imm_ptl.core.McHelper;
import qouteall.q_misc_util.Helper;

import javax.annotation.Nullable;

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
    public boolean adjustPositionAfterTeleport = false;
    
    public boolean bindCluster = false;
    
    @Nullable
    public Portal reversePortal;
    @Nullable
    public Portal flippedPortal;
    @Nullable
    public Portal parallelPortal;
    
    public PortalExtension() {
    
    }
    
    private void readFromNbt(NbtCompound compoundTag) {
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
            adjustPositionAfterTeleport = false;
        }
        
        if (compoundTag.contains("bindCluster")) {
            bindCluster = compoundTag.getBoolean("bindCluster");
        }
        else {
            bindCluster = false;
        }
    }
    
    private void writeToNbt(NbtCompound compoundTag) {
        if (motionAffinity != 0) {
            compoundTag.putDouble("motionAffinity", motionAffinity);
        }
        compoundTag.putBoolean("adjustPositionAfterTeleport", adjustPositionAfterTeleport);
        compoundTag.putBoolean("bindCluster", bindCluster);
    }
    
    private void tick(Portal portal) {
        if (portal.world.isClient()) {
            tickClient(portal);
        }
        else {
            updateClusterStatus(portal);
        }
    }
    
    @Environment(EnvType.CLIENT)
    private void tickClient(Portal portal) {
    
    }
    
    private void updateClusterStatus(Portal portal) {
        if (bindCluster) {
            flippedPortal = Helper.getFirstNullable(McHelper.findEntitiesRough(
                Portal.class,
                portal.getOriginWorld(),
                portal.getOriginPos(),
                0,
                p1 -> p1.getOriginPos().subtract(portal.getOriginPos()).lengthSquared() < 0.5 &&
                    p1.getNormal().dotProduct(portal.getNormal()) < -0.9
            ));
            
            if (flippedPortal != null) {
                PortalExtension.get(flippedPortal).bindCluster = true;
            }
            
            reversePortal = Helper.getFirstNullable(McHelper.findEntitiesRough(
                Portal.class,
                portal.getDestinationWorld(),
                portal.getDestPos(),
                0,
                p1 -> p1.getOriginPos().subtract(portal.getDestPos()).lengthSquared() < 0.5 &&
                    p1.getNormal().dotProduct(portal.getContentDirection()) > 0.9
            ));
            
            if (reversePortal != null) {
                PortalExtension.get(reversePortal).bindCluster = true;
            }
            
            parallelPortal = Helper.getFirstNullable(McHelper.findEntitiesRough(
                Portal.class,
                portal.getDestinationWorld(),
                portal.getDestPos(),
                0,
                p1 -> p1.getOriginPos().subtract(portal.getDestPos()).lengthSquared() < 0.5 &&
                    p1.getNormal().dotProduct(portal.getContentDirection()) < -0.9
            ));
            
            if (parallelPortal != null) {
                PortalExtension.get(parallelPortal).bindCluster = true;
            }
        }
        else {
            flippedPortal = null;
            reversePortal = null;
            parallelPortal = null;
        }
    }
    
    public void rectifyClusterPortals(Portal portal) {
        if (bindCluster) {
            if (flippedPortal != null) {
                flippedPortal.setOriginPos(portal.getOriginPos());
                flippedPortal.setDestination(portal.getDestPos());
                
                flippedPortal.axisW = portal.axisW;
                flippedPortal.axisH = portal.axisH.multiply(-1);
                
                flippedPortal.scaling = portal.scaling;
                flippedPortal.rotation = portal.rotation.copy();
                
                flippedPortal.reloadAndSyncToClient();
            }
            
            if (reversePortal != null) {
                reversePortal.setOriginPos(portal.getDestPos());
                reversePortal.setDestination(portal.getOriginPos());
                
                reversePortal.axisW = portal.transformLocalVecNonScale(portal.axisW);
                reversePortal.axisH = portal.transformLocalVecNonScale(portal.axisH.multiply(-1));
                reversePortal.scaling = 1.0 / portal.scaling;
                if (portal.rotation != null) {
                    reversePortal.rotation = portal.rotation.copy();
                    reversePortal.rotation.conjugate();
                }
                else {
                    reversePortal.rotation = null;
                }
                
                reversePortal.reloadAndSyncToClient();
            }
            
            if (parallelPortal != null) {
                parallelPortal.setOriginPos(portal.getDestPos());
                parallelPortal.setDestination(portal.getOriginPos());
                
                parallelPortal.axisW = portal.transformLocalVecNonScale(portal.axisW);
                parallelPortal.axisH = portal.transformLocalVecNonScale(portal.axisH);
                parallelPortal.scaling = 1.0 / portal.scaling;
                if (portal.rotation != null) {
                    parallelPortal.rotation = portal.rotation.copy();
                    parallelPortal.rotation.conjugate();
                }
                else {
                    parallelPortal.rotation = null;
                }
                
                parallelPortal.reloadAndSyncToClient();
            }
        }
    }
    
}
