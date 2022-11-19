package qouteall.imm_ptl.core.portal.animation;

import net.minecraft.network.chat.Component;
import qouteall.imm_ptl.core.portal.IntraClusterRelation;
import qouteall.imm_ptl.core.portal.Portal;

import java.util.List;

public record AnimationView(
    Portal viewedPortal,
    Portal animationHolder,
    IntraClusterRelation relationToHolder
) {
    
    public List<PortalAnimationDriver> getThisSideAnimations() {
        if (relationToHolder.isReverse) {
            return animationHolder.animation.otherSideAnimations;
        }
        else {
            return animationHolder.animation.thisSideAnimations;
        }
    }
    
    public List<PortalAnimationDriver> getOtherSideAnimations() {
        if (relationToHolder.isReverse) {
            return animationHolder.animation.thisSideAnimations;
        }
        else {
            return animationHolder.animation.otherSideAnimations;
        }
    }
    
    public PortalAnimationDriver convertToHolderAnimation(
        PortalAnimationDriver driver
    ) {
        if (relationToHolder.isFlipped) {
            return driver.getFlippedVersion();
        }
        return driver;
    }
    
    public void addThisSideAnimation(PortalAnimationDriver driver) {
        getThisSideAnimations().add(convertToHolderAnimation(driver));
    }
    
    public void addOtherSideAnimation(PortalAnimationDriver driver) {
        getOtherSideAnimations().add(convertToHolderAnimation(driver));
    }
    
    public Component getInfo() {
        return animationHolder.animation.getInfo(animationHolder, relationToHolder.isReverse);
    }
}
