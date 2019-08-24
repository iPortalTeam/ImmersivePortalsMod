package com.qouteall.immersive_portals.optifine_compatibility;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.my_util.Helper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.dimension.DimensionType;
import net.optifine.shaders.Shaders;

import java.util.HashMap;
import java.util.Map;

public class ShaderContextManager {
    private Map<DimensionType, PerDimensionContext> managedContext = new HashMap<>();
    private Map<DimensionType, Boolean> isStartuped = new HashMap<>();
    
    //null indicates that the context is not switched
    private DimensionType currentContextDimension;
    
    private PerDimensionContext recordedOriginalContext;
    
    public static boolean doUseDuplicateContextInCurrentDimension = true;
    
    public ShaderContextManager() {
        ModMain.preRenderSignal.connectWithWeakRef(
            this,
            this_ -> {
                if (MinecraftClient.getInstance().world != null) {
                    this_.initIfNeeded();
                }
            }
        );
    }
    
    public boolean isContextSwitched() {
        return currentContextDimension != null;
    }
    
    private void initIfNeeded() {
    }
    
    public void cleanup() {
        managedContext.keySet().forEach(
            dimension -> forceSwitchContextAndRun(dimension, Shaders::uninit)
        );
        managedContext.clear();
        isStartuped.clear();
        recordedOriginalContext = null;
    }
    
    public PerDimensionContext getOrCreateContext(DimensionType dimension) {
        if (!doUseDuplicateContextInCurrentDimension) {
            if (dimension == CHelper.getOriginalDimension()) {
                return recordedOriginalContext;
            }
        }
        return managedContext.computeIfAbsent(
            dimension, k -> {
                Helper.log("Context object created " + k);
                return new PerDimensionContext(false);
            }
        );
        
    }
    
    public void switchContextAndRun(Runnable func) {
        DimensionType originalContextDimension = this.currentContextDimension;
        DimensionType dimensionToSiwtchTo = MinecraftClient.getInstance().world.dimension.getType();
        
        if (this.currentContextDimension == null) {
            //currently the context was not switched
            this.currentContextDimension = CHelper.getOriginalDimension();
            recordedOriginalContext = new PerDimensionContext(true);
            ShaderUtils.copyContextToObject.accept(recordedOriginalContext);
            isStartuped.put(currentContextDimension, true);
        }
        
        if (currentContextDimension == dimensionToSiwtchTo) {
            func.run();
        }
        else {
            currentContextDimension = dimensionToSiwtchTo;
            forceSwitchContextAndRun(dimensionToSiwtchTo, func);
        }
        
        currentContextDimension = originalContextDimension;
    }
    
    public void forceSwitchContextAndRun(DimensionType dimension, Runnable func) {
        PerDimensionContext originalContext = new PerDimensionContext(true);
        ShaderUtils.copyContextToObject.accept(originalContext);
        
        PerDimensionContext newContext = getOrCreateContext(dimension);
        ShaderUtils.copyContextFromObject.accept(newContext);
        
        func.run();
    
        ShaderUtils.copyContextToObject.accept(newContext);
    
        ShaderUtils.copyContextFromObject.accept(originalContext);
    }
    
    public void startupIfNecessary(DimensionType dimension) {
        if (dimension != CHelper.getOriginalDimension()) {
            if (!isStartuped.getOrDefault(dimension, false)) {
                Shaders.startup(MinecraftClient.getInstance());
                isStartuped.put(dimension, true);
                Helper.log("Startuped secondary context " + dimension);
            }
        }
    }
    
    public boolean isCurrentDimensionRendered() {
        if (currentContextDimension == null) {
            return false;
        }
        return CGlobal.renderer.isDimensionRendered(currentContextDimension);
    }
    
    public void onPlayerTraveled(DimensionType from, DimensionType to) {
        assert !managedContext.containsKey(from);
        PerDimensionContext oldContext = new PerDimensionContext(true);
        ShaderUtils.copyContextToObject.accept(oldContext);
        managedContext.put(from, oldContext);
        
        PerDimensionContext newContext = managedContext.get(to);
        ShaderUtils.copyContextFromObject.accept(newContext);
        managedContext.remove(to);
    }
}
