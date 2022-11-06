package qouteall.imm_ptl.core.portal.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalExtension;
import qouteall.imm_ptl.core.portal.PortalState;
import qouteall.q_misc_util.Helper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

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
    @NotNull
    public List<PortalAnimationDriver> thisSideAnimations = new ArrayList<>();
    @NotNull
    public List<PortalAnimationDriver> otherSideAnimations = new ArrayList<>();
    
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
    
    public void readFromTag(CompoundTag tag) {
        if (tag.contains("animation")) {
            defaultAnimation = DefaultPortalAnimation.fromNbt(tag.getCompound("animation"));
        }
        else if (tag.contains("defaultAnimation")) {
            defaultAnimation = DefaultPortalAnimation.fromNbt(tag.getCompound("defaultAnimation"));
        }
        else {
            defaultAnimation = DefaultPortalAnimation.createDefault();
        }
        
        if (tag.contains("thisSideAnimations")) {
            ListTag listTag = tag.getList("thisSideAnimations", 10);
            thisSideAnimations = Helper.listTagToList(listTag, PortalAnimationDriver::fromTag);
        }
        else {
            thisSideAnimations.clear();
        }
        
        if (tag.contains("otherSideAnimations")) {
            ListTag listTag = tag.getList("otherSideAnimations", 10);
            otherSideAnimations = Helper.listTagToList(listTag, PortalAnimationDriver::fromTag);
        }
        else {
            otherSideAnimations.clear();
        }
    }
    
    public void writeToTag(CompoundTag tag) {
        tag.put("defaultAnimation", defaultAnimation.toNbt());
        
        if (!thisSideAnimations.isEmpty()) {
            tag.put(
                "thisSideAnimations",
                Helper.listToListTag(thisSideAnimations, PortalAnimationDriver::toTag)
            );
        }
        
        if (!otherSideAnimations.isEmpty()) {
            tag.put(
                "otherSideAnimations",
                Helper.listToListTag(otherSideAnimations, PortalAnimationDriver::toTag)
            );
        }
    }
    
    public boolean isRunningRealAnimation() {
        return lastTickAnimatedState != null || thisTickAnimatedState != null || hasRunningAnimationDriver();
    }
    
    public boolean hasRunningAnimationDriver() {
        return !thisSideAnimations.isEmpty() || !otherSideAnimations.isEmpty();
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
            if (hasRunningAnimationDriver()) {
                markRequiresClientAnimationUpdate(portal);
            }
        }
    }
    
    @Environment(EnvType.CLIENT)
    private static void markRequiresClientAnimationUpdate(Portal portal) {
        ClientPortalAnimationManagement.markRequiresCustomAnimationUpdate(portal);
    }
    
    public void updateAnimationDriver(Portal portal, long gameTime, float partialTicks, boolean isTicking) {
        if (hasRunningAnimationDriver()) {
            PortalState portalState = portal.getPortalState();
            if (portalState == null) {
                return;
            }
            UnilateralPortalState thisSide = UnilateralPortalState.extractThisSide(portalState);
            UnilateralPortalState otherSide = UnilateralPortalState.extractOtherSide(portalState);
            
            UnilateralPortalState.Builder thisSideBuilder = new UnilateralPortalState.Builder().from(thisSide);
            UnilateralPortalState.Builder otherSideBuilder = new UnilateralPortalState.Builder().from(otherSide);
            
            int originalThisSideAnimationCount = thisSideAnimations.size();
            int originalOtherSideAnimationCount = otherSideAnimations.size();
            
            thisSideAnimations.removeIf(animationDriver -> {
                boolean finished = animationDriver.update(thisSideBuilder, gameTime, partialTicks);
                return finished;
            });
            
            otherSideAnimations.removeIf(animationDriver -> {
                boolean finished = animationDriver.update(otherSideBuilder, gameTime, partialTicks);
                return finished;
            });
            
            if (thisSideBuilder.dimension != thisSide.dimension() || otherSideBuilder.dimension != otherSide.dimension()) {
                Helper.err("Portal animation driver cannot change dimension");
                portal.clearAnimationDrivers();
                return;
            }
            
            PortalState newPortalState =
                UnilateralPortalState.combine(thisSideBuilder.build(), otherSideBuilder.build());
            portal.setPortalState(newPortalState);
            
            if (isTicking) {
                portal.animation.thisTickAnimatedState = newPortalState;
            }
            
            portal.rectifyClusterPortals(false);
            if (isTicking) {
                PortalExtension extension = PortalExtension.get(portal);
                updateThisTickAnimatedState(extension.flippedPortal);
                updateThisTickAnimatedState(extension.reversePortal);
                updateThisTickAnimatedState(extension.parallelPortal);
            }
            
            if (!portal.level.isClientSide()) {
                if (thisSideAnimations.size() != originalThisSideAnimationCount ||
                    otherSideAnimations.size() != originalOtherSideAnimationCount
                ) {
                    PortalExtension.forClusterPortals(portal, Portal::reloadAndSyncToClientNextTick);
                }
            }
        }
    }
    
    public void addThisSideAnimationDriver(Portal portal, PortalAnimationDriver animationDriver) {
        PortalExtension extension = PortalExtension.get(portal);
        if (extension.flippedPortal != null) {
            if (extension.flippedPortal.animation.hasRunningAnimationDriver()) {
                extension.flippedPortal.animation.thisSideAnimations.add(animationDriver);
                extension.flippedPortal.reloadAndSyncToClient();
                return;
            }
        }
        
        if (extension.reversePortal != null) {
            if (extension.reversePortal.animation.hasRunningAnimationDriver()) {
                extension.reversePortal.animation.otherSideAnimations.add(animationDriver);
                extension.reversePortal.reloadAndSyncToClient();
                return;
            }
        }
        
        if (extension.parallelPortal != null) {
            if (extension.parallelPortal.animation.hasRunningAnimationDriver()) {
                extension.parallelPortal.animation.otherSideAnimations.add(animationDriver);
                extension.parallelPortal.reloadAndSyncToClient();
                return;
            }
        }
        
        thisSideAnimations.add(animationDriver);
        portal.reloadAndSyncToClient();
    }
    
    public void addOtherSideAnimationDriver(Portal portal, PortalAnimationDriver animationDriver) {
        PortalExtension extension = PortalExtension.get(portal);
        if (extension.flippedPortal != null) {
            if (extension.flippedPortal.animation.hasRunningAnimationDriver()) {
                extension.flippedPortal.animation.otherSideAnimations.add(animationDriver);
                extension.flippedPortal.reloadAndSyncToClient();
                return;
            }
        }
        
        if (extension.reversePortal != null) {
            if (extension.reversePortal.animation.hasRunningAnimationDriver()) {
                extension.reversePortal.animation.thisSideAnimations.add(animationDriver);
                extension.reversePortal.reloadAndSyncToClient();
                return;
            }
        }
        
        if (extension.parallelPortal != null) {
            if (extension.parallelPortal.animation.hasRunningAnimationDriver()) {
                extension.parallelPortal.animation.thisSideAnimations.add(animationDriver);
                extension.parallelPortal.reloadAndSyncToClient();
                return;
            }
        }
        
        otherSideAnimations.add(animationDriver);
        portal.reloadAndSyncToClient();
    }
    
    public void clearAnimationDrivers(Portal portal, boolean clearThisSide, boolean clearOtherSide) {
        Validate.isTrue(!portal.level.isClientSide());
        
        if (thisSideAnimations.isEmpty() && otherSideAnimations.isEmpty()) {
            return;
        }
        
        PortalState portalState = portal.getPortalState();
        assert portalState != null;
        UnilateralPortalState thisSideState = UnilateralPortalState.extractThisSide(portalState);
        UnilateralPortalState otherSideState = UnilateralPortalState.extractOtherSide(portalState);
        UnilateralPortalState.Builder from = new UnilateralPortalState.Builder().from(thisSideState);
        UnilateralPortalState.Builder to = new UnilateralPortalState.Builder().from(otherSideState);
        
        if (clearThisSide) {
            for (PortalAnimationDriver animationDriver : thisSideAnimations) {
                animationDriver.halt(from, portal.level.getGameTime());
            }
            thisSideAnimations.clear();
        }
        
        if (clearOtherSide) {
            for (PortalAnimationDriver animationDriver : otherSideAnimations) {
                animationDriver.halt(to, portal.level.getGameTime());
            }
            otherSideAnimations.clear();
        }
        
        PortalState newState = UnilateralPortalState.combine(from.build(), to.build());
        portal.setPortalState(newState);
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
                secondaryPortal.clearAnimationDrivers();
            }
            secondaryPortal.animation.thisTickAnimatedState = secondaryPortal.getPortalState();
        }
    }
}
