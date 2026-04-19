package qouteall.imm_ptl.core.mixin.client.render;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.render.TransformationManager;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.LimitedLogger;

@Mixin(Frustum.class)
public abstract class MixinFrustum_FixDeadLoop {

    @Unique
    private static final LimitedLogger limitedLogger = new LimitedLogger(10);

    /**
     * Make it to not deadloop when using isometric view.
     */
    @Inject(method = "offsetToFullyIncludeCameraCube", at = @At("HEAD"), cancellable = true)
    public void initButSkipIfIsometric(int i, CallbackInfoReturnable<Frustum> cir, @Share("countLimit") LocalIntRef countLimit) {
        if (TransformationManager.isIsometricView) {
            cir.setReturnValue((Frustum) (Object) this);
        }
        countLimit.set(10+1);
    }

    /**
     * Also make it to not deadloop even if the projection matrix is broken. (In normal cases the projection should not be broken.)
     */
    @Definition(id = "intersectAab", method = "Lorg/joml/FrustumIntersection;intersectAab(FFFFFF)I")
    @Expression("?.intersectAab(?, ?, ?, ?, ?, ?) != ?")
    @ModifyExpressionValue(
            method = "offsetToFullyIncludeCameraCube",
            at = @At("MIXINEXTRAS:EXPRESSION")
    )
    public boolean decrementFrustumLoopCount(boolean shouldContinue, @Share("countLimit") LocalIntRef countLimit) {
        countLimit.set(countLimit.get()-1);
        if (countLimit.get() <= 0) {
            limitedLogger.invoke(() -> {
                Helper.err("the projection matrix and the frustum are abnormal");
                new Throwable().printStackTrace();
            });
            return false;
        }
        return shouldContinue;
    }
}
