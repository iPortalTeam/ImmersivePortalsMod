package qouteall.imm_ptl.core.portal.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalExtension;
import qouteall.imm_ptl.core.portal.PortalState;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.q_misc_util.Helper;

import javax.annotation.Nullable;
import java.util.function.BooleanSupplier;

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
    PortalState lastTickAnimatedState;
    @Nullable
    PortalState thisTickAnimatedState;
    
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
        return lastTickAnimatedState != null || thisTickAnimatedState != null || animationDriver != null;
    }
    
    public void tick(Portal portal) {
        lastTickAnimatedState = thisTickAnimatedState;
        thisTickAnimatedState = null;
        
        if (animationDriver != null) {
            boolean finishes = updateAnimationDriverAndGetIsFinished(portal);
            portal.animation.thisTickAnimatedState = portal.getPortalState();
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
    
    /**
     * The server ticking process {@link ServerLevel#tick(BooleanSupplier)}:
     * 1. increase game time
     * 2. tick entities (including portals)
     * So use 1 as partial tick
     */
    private boolean updateAnimationDriverAndGetIsFinished(Portal portal) {
        assert animationDriver != null;
        
        if (portal.level.isClientSide()) {
            return updateAnimationDriverAndGetIsFinishedClient(portal);
        }
        else {
            return animationDriver.update(portal, portal.level.getGameTime(), 1);
        }
    }
    
    /**
     * The client ticking process {@link Minecraft#tick()}
     * 1. Tick entities (including portals)
     * 2. Increase game time
     * So use 0 as partial tick
     */
    @Environment(EnvType.CLIENT)
    private boolean updateAnimationDriverAndGetIsFinishedClient(Portal portal) {
        assert animationDriver != null;
        
        ClientPortalAnimationManagement.markRequiresCustomAnimationUpdate(portal);
        
        return animationDriver.update(
            portal,
            StableClientTimer.getStableTickTime(),
            0
        );
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
            if (secondaryPortal.animation.thisTickAnimatedState != null) {
                Helper.log("Conflicting animation in " + secondaryPortal);
                secondaryPortal.setAnimationDriver(null);
            }
            secondaryPortal.animation.thisTickAnimatedState = secondaryPortal.getPortalState();
        }
    }
}
