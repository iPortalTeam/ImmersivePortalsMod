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
    private PerDimensionContext templateContext;
    
    private boolean isCleaningUp = false;
    
    public static boolean doUseDuplicateContextInCurrentDimension = true;
    public static boolean doUseTemplate = true;
    
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
        isCleaningUp = true;
    
        Helper.log("start cleaning shader context " + managedContext.keySet());
        
        managedContext.keySet().forEach(
            dimension -> {
                PerDimensionContext context = getOrCreateContext(dimension);
                forceSwitchToContextAndRun(context, Shaders::uninit);
            }
        );
        managedContext.clear();
        isStartuped.clear();
        recordedOriginalContext = null;
        currentContextDimension = null;
    
        isCleaningUp = false;
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
                if (doUseTemplate) {
                    return createContextByTemplate();
                }
                else {
                    PerDimensionContext context = new PerDimensionContext();
                    context.doDefaultInit();
                    return context;
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
            isStartuped.put(currentContextDimension, true);
        }
    
        if (currentContextDimension == dimensionToSwitchTo) {
            func.run();
        }
        else {
            currentContextDimension = dimensionToSwitchTo;
            PerDimensionContext newContext = getOrCreateContext(dimensionToSwitchTo);
        
            forceSwitchToContextAndRun(newContext, () -> {
                startupIfNecessary(dimensionToSwitchTo);
                func.run();
            });
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
    
    public void startupIfNecessary(DimensionType dimension) {
        if (doUseTemplate) {
            return;
        }
        if (dimension != CHelper.getOriginalDimension()) {
            if (!isStartuped.getOrDefault(dimension, false)) {
                Helper.log("Start Startuping secondary context " + dimension);
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
    
        isStartuped.put(from, true);
    
        PerDimensionContext oldContext = new PerDimensionContext();
        OFGlobal.copyContextToObject.accept(oldContext);
        managedContext.put(from, oldContext);
        
        PerDimensionContext newContext = managedContext.get(to);
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
        if (doUseTemplate) {
            assert !isContextSwitched();
        
            Shaders.uninit();
        
            templateContext = new PerDimensionContext();
        
            OFGlobal.copyContextToObject.accept(templateContext);
        }
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
