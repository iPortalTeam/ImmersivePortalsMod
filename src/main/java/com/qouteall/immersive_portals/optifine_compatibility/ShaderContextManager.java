package com.qouteall.immersive_portals.optifine_compatibility;

import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.optifine.shaders.Shaders;

import java.util.HashMap;
import java.util.Map;

public class ShaderContextManager {
    private Map<DimensionType, PerDimensionContext> managedContext = new HashMap<>();
    private Map<DimensionType, PerDimensionContext> abundantContext = new HashMap<>();
    
    //null indicates that the context is not switched
    private DimensionType currentContextDimension;
    
    private PerDimensionContext recordedOriginalContext = new PerDimensionContext();
    
    private PerDimensionContext templateContext;
    
    private boolean isCleaningUp = false;
    
    public static boolean doUseDuplicateContextForCurrentDimension = false;
    
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
        recordedOriginalContext = new PerDimensionContext();
        currentContextDimension = null;
        
        isCleaningUp = false;
    }
    
    public PerDimensionContext getOrCreateContext(DimensionType dimension) {
        if (!doUseDuplicateContextForCurrentDimension) {
            if (isContextSwitched()) {
                if (dimension == CHelper.getOriginalDimension()) {
                    return recordedOriginalContext;
                }
            }
        }
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
        DimensionType oldContextDimension = this.currentContextDimension;
        ClientWorld currentClientWorld = MinecraftClient.getInstance().world;
        DimensionType dimensionToSwitchTo = currentClientWorld.dimension.getType();
        
        if (currentContextDimension == null) {
            //currently the context was not switched
            currentContextDimension = CHelper.getOriginalDimension();
            OFGlobal.copyContextToObject.accept(recordedOriginalContext);
        }
        
        if (currentContextDimension == dimensionToSwitchTo) {
            check(dimensionToSwitchTo);
            func.run();
        }
        else {
            currentContextDimension = dimensionToSwitchTo;
            PerDimensionContext newContext = getOrCreateContext(dimensionToSwitchTo);
            
            if (newContext.currentWorld != null) {
                checkState(currentClientWorld, dimensionToSwitchTo, newContext);
            }
            
            forceSwitchToContextAndRun(newContext, func);
        }
        
        currentContextDimension = oldContextDimension;
    }
    
    private void check(DimensionType dimensionToSwitchTo) {
        World shadersCurrentWorld = OFGlobal.getCurrentWorld.get();
        if (shadersCurrentWorld != null) {
            DimensionType shaderCurrentDimension = shadersCurrentWorld.dimension.getType();
            if (shaderCurrentDimension != dimensionToSwitchTo) {
                Helper.err(
                    "Shader Context Abnormal. Shader: " +
                        shaderCurrentDimension +
                        "Main: " +
                        dimensionToSwitchTo
                );
            }
        }
    }
    
    private void checkState(
        ClientWorld currentClientWorld,
        DimensionType dimensionToSwitchTo,
        PerDimensionContext newContext
    ) {
        DimensionType newContextDimension = newContext.currentWorld.dimension.getType();
        
        if (newContextDimension != dimensionToSwitchTo) {
            Helper.err(
                "Shader Context Abnormal. Shader: " +
                    newContextDimension +
                    "Main: " +
                    dimensionToSwitchTo
            );
            newContext.currentWorld = currentClientWorld;
            Helper.log("Force corrected");
            forceCorrectedNum++;
            if (forceCorrectedNum > 100) {
                throw new IllegalStateException();
            }
        }
    }
    
    int forceCorrectedNum = 0;
    
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
        return RenderStates.isDimensionRendered(currentContextDimension);
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
        
        check(to);
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
        
        Helper.log("shader context template updated");
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
