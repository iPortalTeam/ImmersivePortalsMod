package qouteall.imm_ptl.core.portal.animation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import qouteall.q_misc_util.Helper;

public class SizeAnimation implements PortalAnimationDriver {
    
    public static void init() {
        PortalAnimationDriver.registerDeserializer(
            new ResourceLocation("imm_ptl:size"),
            SizeAnimation::deserialize
        );
    }
    
    public final double fromWidth;
    public final double fromHeight;
    public final double toWidth;
    public final double toHeight;
    public final long startGameTime;
    public final long endGameTime;
    
    public SizeAnimation(
        double fromWidth, double fromHeight,
        double toWidth, double toHeight,
        long startGameTime, long endGameTime
    ) {
        this.fromWidth = fromWidth;
        this.fromHeight = fromHeight;
        this.toWidth = toWidth;
        this.toHeight = toHeight;
        this.startGameTime = startGameTime;
        this.endGameTime = endGameTime;
    }
    
    private static SizeAnimation deserialize(CompoundTag tag) {
        double fromWidth = tag.getDouble("fromWidth");
        double fromHeight = tag.getDouble("fromHeight");
        double toWidth = tag.getDouble("toWidth");
        double toHeight = tag.getDouble("toHeight");
        long startGameTime = tag.getLong("startGameTime");
        long endGameTime = tag.getLong("endGameTime");
        
        return new SizeAnimation(
            fromWidth, fromHeight,
            toWidth, toHeight,
            startGameTime, endGameTime
        );
    }
    
    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        
        tag.putString("type", "imm_ptl:size");
        tag.putDouble("fromWidth", fromWidth);
        tag.putDouble("fromHeight", fromHeight);
        tag.putDouble("toWidth", toWidth);
        tag.putDouble("toHeight", toHeight);
        tag.putLong("startGameTime", startGameTime);
        tag.putLong("endGameTime", endGameTime);
        
        return tag;
    }
    
    @Override
    public boolean update(UnilateralPortalState.Builder stateBuilder, long tickTime, float partialTicks) {
        
        double progress = (tickTime - startGameTime + partialTicks) / (double) (endGameTime - startGameTime);
        
        if (progress >= 1) {
            stateBuilder.width(toWidth);
            stateBuilder.height(toHeight);
            return true;
        }
        else {
            stateBuilder.width(Mth.lerp(progress, fromWidth, toWidth));
            stateBuilder.height(Mth.lerp(progress, fromHeight, toHeight));
            return false;
        }
    }
}
