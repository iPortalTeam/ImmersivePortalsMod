package com.qouteall.immersive_portals.optifine_compatibility;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.my_util.Helper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.dimension.DimensionType;
import net.optifine.shaders.Shaders;

import java.util.HashMap;
import java.util.Map;

public class ShaderContextManager {
    private Map<DimensionType, PerDimensionContext> managedContext = new HashMap<>();
    private Map<DimensionType, PerDimensionContext> abundantContext = new HashMap<>();
    
    //null indicates that the context is not switched
    private DimensionType currentContextDimension;
    
    private PerDimensionContext recordedOriginalContext;
    private PerDimensionContext templateContext;
    
    private boolean isCleaningUp = false;
    
    public ShaderContextManager() {
    
    }
    
    public boolean isContextSwitched() {
        return currentContextDimension != null;
    }
    
    public void cleanup() {
        isCleaningUp = true;
    
        Helper.log("start cleaning shader context " + managedContext.keySet());
    
        managedContext.forEach(
            (dimension, context) -> {
                forceSwitchToContextAndRun(context, Shaders::uninit);
            }
        );
        managedContext.clear();
        abundantContext.forEach(
            (dimension, context) -> {
                forceSwitchToContextAndRun(context, Shaders::uninit);
            }
        );
        abundantContext.clear();
        recordedOriginalContext = null;
        currentContextDimension = null;
    
        isCleaningUp = false;
    }
    
    public PerDimensionContext getOrCreateContext(DimensionType dimension) {
        return managedContext.computeIfAbsent(
            dimension, k -> {
                if (abundantContext.containsKey(dimension)) {
                    Helper.log("Employed abundant context" + k);
                    return abundantContext.remove(dimension);
                }
                else {
                    Helper.log("Context object created " + k);
                    return createContextByTemplate();
                }
            }
        );
        
    }
    
    public void switchContextAndRun(Runnable func) {
        DimensionType originalContextDimension = this.currentContextDimension;
        DimensionType dimensionToSwitchTo = MinecraftClient.getInstance().world.dimension.getType();
        
        if (this.currentContextDimension == null) {
            //currently the context was not switched
            this.currentContextDimension = CHelper.getOriginalDimension();
            recordedOriginalContext = new PerDimensionContext();
            OFGlobal.copyContextToObject.accept(recordedOriginalContext);
        }
    
        if (currentContextDimension == dimensionToSwitchTo) {
            func.run();
        }
        else {
            currentContextDimension = dimensionToSwitchTo;
            PerDimensionContext newContext = getOrCreateContext(dimensionToSwitchTo);
        
            forceSwitchToContextAndRun(newContext, func);
        }
        
        currentContextDimension = originalContextDimension;
    }
    
    private void forceSwitchToContextAndRun(
        PerDimensionContext contextToSwitchTo,
        Runnable func
    ) {
        PerDimensionContext originalContext = new PerDimensionContext();
        OFGlobal.copyContextToObject.accept(originalContext);
        
        OFGlobal.copyContextFromObject.accept(contextToSwitchTo);
        
        try {
            func.run();
        }
        finally {
            OFGlobal.copyContextToObject.accept(contextToSwitchTo);
            
            OFGlobal.copyContextFromObject.accept(originalContext);
        }
    }
    
    public boolean isCurrentDimensionRendered() {
        if (currentContextDimension == null) {
            return false;
        }
        return CGlobal.renderer.isDimensionRendered(currentContextDimension);
    }
    
    public void onPlayerTraveled(DimensionType from, DimensionType to) {
        if (managedContext.containsKey(from)) {
            assert !abundantContext.containsKey(from);
            abundantContext.put(from, managedContext.remove(from));
        }
        
        PerDimensionContext oldContext = new PerDimensionContext();
        OFGlobal.copyContextToObject.accept(oldContext);
        managedContext.put(from, oldContext);
    
        PerDimensionContext newContext = getOrCreateContext(to);
        OFGlobal.copyContextFromObject.accept(newContext);
        managedContext.remove(to);
    }
    
    public void onShaderUninit() {
        if (!isCleaningUp) {
            cleanup();
        }
    }
    
    //we need to record the template when shader is loaded but not initialized
    public void updateTemplateContext() {
        assert !isContextSwitched();
    
        Shaders.uninit();
        cleanup();
    
        templateContext = new PerDimensionContext();
    
        OFGlobal.copyContextToObject.accept(templateContext);
    
        Helper.log("context template updated");
    }
    
    private PerDimensionContext createContextByTemplate() {
        assert templateContext != null;
        PerDimensionContext newContext = new PerDimensionContext();
        forceSwitchToContextAndRun(templateContext, () -> {
            OFGlobal.copyContextToObject.accept(newContext);
        });
        newContext.doSpecialInit();
        return newContext;
    }
}
