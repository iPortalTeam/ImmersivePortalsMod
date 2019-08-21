package com.qouteall.immersive_portals;

import net.fabricmc.loader.FabricLoader;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinPluginWithOptifine implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
    
    }
    
    @Override
    public String getRefMapperConfig() {
        return null;
    }
    
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        //don't use CGlobal.isOptifinePresent;
        return FabricLoader.INSTANCE.isModLoaded("optifabric");
    }
    
    @Override
    public void acceptTargets(
        Set<String> myTargets, Set<String> otherTargets
    ) {
    
    }
    
    @Override
    public List<String> getMixins() {
        return null;
    }
    
    @Override
    public void preApply(
        String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo
    ) {
    
    }
    
    @Override
    public void postApply(
        String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo
    ) {
    
    }
}
