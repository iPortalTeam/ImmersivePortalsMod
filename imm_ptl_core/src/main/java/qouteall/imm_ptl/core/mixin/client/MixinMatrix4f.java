package qouteall.imm_ptl.core.mixin.client;

import qouteall.imm_ptl.core.ducks.IEMatrix4f;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

//mojang does not provide a method to load numbers into matrix
@Mixin(Matrix4f.class)
public class MixinMatrix4f implements IEMatrix4f {
    @Shadow
    float a00;
    @Shadow
    float a01;
    @Shadow
    float a02;
    @Shadow
    float a03;
    @Shadow
    float a10;
    @Shadow
    float a11;
    @Shadow
    float a12;
    @Shadow
    float a13;
    @Shadow
    float a20;
    @Shadow
    float a21;
    @Shadow
    float a22;
    @Shadow
    float a23;
    @Shadow
    float a30;
    @Shadow
    float a31;
    @Shadow
    float a32;
    @Shadow
    float a33;
    
    @Override
    public void loadFromArray(float[] arr) {
        a00 = arr[0];
        a01 = arr[1];
        a02 = arr[2];
        a03 = arr[3];
        a10 = arr[4];
        a11 = arr[5];
        a12 = arr[6];
        a13 = arr[7];
        a20 = arr[8];
        a21 = arr[9];
        a22 = arr[10];
        a23 = arr[11];
        a30 = arr[12];
        a31 = arr[13];
        a32 = arr[14];
        a33 = arr[15];
    }
    
    @Override
    public void loadToArray(float[] arr) {
        arr[0] = a00;
        arr[1] = a01;
        arr[2] = a02;
        arr[3] = a03;
        arr[4] = a10;
        arr[5] = a11;
        arr[6] = a12;
        arr[7] = a13;
        arr[8] = a20;
        arr[9] = a21;
        arr[10] = a22;
        arr[11] = a23;
        arr[12] = a30;
        arr[13] = a31;
        arr[14] = a32;
        arr[15] = a33;
    }
}
