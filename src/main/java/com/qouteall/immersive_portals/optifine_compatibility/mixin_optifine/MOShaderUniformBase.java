package com.qouteall.immersive_portals.optifine_compatibility.mixin_optifine;

import net.optifine.shaders.uniform.ShaderUniformBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ShaderUniformBase.class)
public class MOShaderUniformBase {
    @Shadow
    private int program;
    
    @Shadow
    private int[] locations;
    
    @Shadow
    private String name;
//
//    //optifine code does not consider the situration that the shader object is not fully
//    //finished and the uniform generation will be failed
//    //but the shader will be initialized afterwards.
//    @Overwrite
//    public int getLocation() {
//        if (this.program <= 0) {
//            return -1;
//        } else {
//            int location = this.locations[this.program];
//            if (location == Integer.MIN_VALUE) {
//                location = ARBShaderObjects.glGetUniformLocationARB(this.program, this.name);
//                if (location != -1) {
//                    this.locations[this.program] = location;
//                }
//            }
//
//            return location;
//        }
//    }
}
