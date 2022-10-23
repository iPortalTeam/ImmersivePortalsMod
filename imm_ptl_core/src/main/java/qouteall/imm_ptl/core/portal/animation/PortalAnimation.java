package qouteall.imm_ptl.core.portal.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.NotNull;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalExtension;
import qouteall.imm_ptl.core.portal.PortalState;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.q_misc_util.Helper;

import javax.annotation.Nullable;

public class PortalAnimation {
    /**
     * The default animation is triggered when changing portal
     * The default animation is client-only. On the server side it changes abruptly.
     * The default animation cannot do relative teleportation.
     */
    @NotNull
    public DefaultPortalAnimation defaultAnimation = DefaultPortalAnimation.createDefault();
    
    /**
     * The animation driver controls the real animation.
     * The real animation runs on both sides and can do relative teleportation.
     */
    @Nullable
    public PortalAnimationDriver animationDriver;
    
    public boolean lastTickRealAnimated = false;
    public boolean thisTickRealAnimated = false;
    
    // for client player teleportation
    @Environment(EnvType.CLIENT)
    @Nullable
    public PortalState clientLastPortalState;
    @Environment(EnvType.CLIENT)
    public long clientLastPortalStateCounter = -1;
    @Environment(EnvType.CLIENT)
    @Nullable
    public PortalState clientCurrentPortalState;
    @Environment(EnvType.CLIENT)
    public long clientCurrentPortalStateCounter = -1;
    
    public boolean isRunningRealAnimation() {
        return lastTickRealAnimated || thisTickRealAnimated || animationDriver != null;
    }
    
    public void tick(Portal portal) {
        lastTickRealAnimated = thisTickRealAnimated;
        thisTickRealAnimated = false;
        
        if (portal.level.isClientSide()) {
            tickAnimationDriverClient(portal);
        }
        else {
            tickAnimationDriverServer(portal);
        }
    }
    
    private void tickAnimationDriverServer(Portal portal) {
        if (animationDriver != null) {
            boolean finishes = animationDriver.update(portal, portal.level.getGameTime(), 0);
            portal.animation.thisTickRealAnimated = true;
            if (animationDriver.shouldRectifyCluster()) {
                portal.rectifyClusterPortals();
                PortalExtension extension = PortalExtension.get(portal);
                updateAndCheckAnimationStatus(extension.flippedPortal);
                updateAndCheckAnimationStatus(extension.reversePortal);
                updateAndCheckAnimationStatus(extension.parallelPortal);
            }
            if (finishes) {
                animationDriver = null;
            }
        }
    }
    
    @Environment(EnvType.CLIENT)
    private void tickAnimationDriverClient(Portal portal) {
        if (animationDriver != null) {
            ClientPortalAnimationManagement.markRequiresCustomAnimationUpdate(portal);
        }
    }
    
    public void setAnimationDriver(Portal portal, @Nullable PortalAnimationDriver driver) {
        if (driver == animationDriver) {
            return;
        }
        
        if (!portal.level.isClientSide()) {
            if (animationDriver != null) {
                animationDriver.serverSideForceStop(portal, portal.level.getGameTime());
            }
        }
        
        animationDriver = driver;
        
        if (!portal.level.isClientSide()) {
            portal.reloadAndSyncToClientNextTick();
        }
    }
    
    @Environment(EnvType.CLIENT)
    public void updateClientState(Portal portal, long currentTeleportationCounter) {
        if (currentTeleportationCounter == clientCurrentPortalStateCounter) {
            return;
        }
        
        if (isRunningRealAnimation()) {
            clientLastPortalState = clientCurrentPortalState;
            clientLastPortalStateCounter = clientCurrentPortalStateCounter;
            
            clientCurrentPortalState = portal.getPortalState();
            clientCurrentPortalStateCounter = currentTeleportationCounter;
        }
    }
    
    static void updateAndCheckAnimationStatus(Portal secondaryPortal) {
        if (secondaryPortal != null) {
            if (secondaryPortal.animation.thisTickRealAnimated) {
                Helper.log("Conflicting animation in " + secondaryPortal);
                secondaryPortal.setAnimationDriver(null);
            }
            secondaryPortal.animation.thisTickRealAnimated = true;
        }
    }
}
