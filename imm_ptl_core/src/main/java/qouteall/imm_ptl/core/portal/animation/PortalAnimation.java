package qouteall.imm_ptl.core.portal.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.NotNull;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalExtension;
import qouteall.imm_ptl.core.portal.PortalState;
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
    
    @Nullable
    public PortalState lastTickAnimatedState;
    @Nullable
    public PortalState thisTickAnimatedState;
    
    // for client player teleportation
    @Environment(EnvType.CLIENT)
    @Nullable
    public PortalState clientLastFramePortalState;
    @Environment(EnvType.CLIENT)
    public long clientLastFramePortalStateCounter = -1;
    @Environment(EnvType.CLIENT)
    @Nullable
    public PortalState clientCurrentFramePortalState;
    @Environment(EnvType.CLIENT)
    public long clientCurrentFramePortalStateCounter = -1;
    
    public boolean isRunningRealAnimation() {
        return lastTickAnimatedState != null || thisTickAnimatedState != null || animationDriver != null;
    }
    
    public void tick(Portal portal) {
        lastTickAnimatedState = thisTickAnimatedState;
        thisTickAnimatedState = null;
        
        if (!portal.level.isClientSide()) {
            /*
              The server ticking process {@link ServerLevel#tick(BooleanSupplier)}:
              1. increase game time
              2. tick entities (including portals)
              So use 1 as partial tick
             */
            updateAnimationDriver(portal, portal.level.getGameTime(), 1, true);
        }
        else {
            // handled on client
            markRequiresClientAnimationUpdate(portal);
        }
    }
    
    @Environment(EnvType.CLIENT)
    private static void markRequiresClientAnimationUpdate(Portal portal) {
        ClientPortalAnimationManagement.markRequiresCustomAnimationUpdate(portal);
    }
    
    public void updateAnimationDriver(Portal portal, long gameTime, float partialTicks, boolean isTicking) {
        if (animationDriver != null) {
            boolean finishes = animationDriver.update(portal, gameTime, partialTicks);
            if (isTicking) {
                portal.animation.thisTickAnimatedState = portal.getPortalState();
            }
            if (animationDriver.shouldRectifyCluster()) {
                portal.rectifyClusterPortals();
                if (isTicking) {
                    PortalExtension extension = PortalExtension.get(portal);
                    updateThisTickAnimatedState(extension.flippedPortal);
                    updateThisTickAnimatedState(extension.reversePortal);
                    updateThisTickAnimatedState(extension.parallelPortal);
                }
            }
            if (finishes) {
                animationDriver = null;
            }
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
        if (currentTeleportationCounter == clientCurrentFramePortalStateCounter) {
            return;
        }
        
        if (isRunningRealAnimation()) {
            clientLastFramePortalState = clientCurrentFramePortalState;
            clientLastFramePortalStateCounter = clientCurrentFramePortalStateCounter;
            
            clientCurrentFramePortalState = portal.getPortalState();
            clientCurrentFramePortalStateCounter = currentTeleportationCounter;
        }
    }
    
    static void updateThisTickAnimatedState(Portal secondaryPortal) {
        if (secondaryPortal != null) {
            if (secondaryPortal.animation.thisTickAnimatedState != null) {
                Helper.log("Conflicting animation in " + secondaryPortal);
                secondaryPortal.setAnimationDriver(null);
            }
            secondaryPortal.animation.thisTickAnimatedState = secondaryPortal.getPortalState();
        }
    }
}
