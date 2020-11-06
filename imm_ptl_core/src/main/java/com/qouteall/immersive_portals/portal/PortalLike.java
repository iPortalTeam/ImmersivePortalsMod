package com.qouteall.immersive_portals.portal;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public interface PortalLike {
    boolean l_isConventionalPortal();
    
    // bounding box
    Box l_getAreaBox();
    
    Vec3d temp_getOriginPos();
    
    Vec3d temp_getDestPos();
    
    World l_getOriginWorld();
    
    World l_getDestWorld();
    
    boolean l_isRoughlyVisibleTo(Vec3d cameraPos);
    
    Vec3d l_getContentDirection();
    
    @Nullable
    Quaternion l_getRotation();
    
    double l_getScale();
    
    boolean l_getIsMirror();
    
    boolean l_getIsGlobal();
    
    // used for advanced frustum culling
    @Nullable
    Vec3d[] getInnerFrustumCullingVertices();
    
    // used for super advanced frustum culling
    @Nullable
    Vec3d[] getOuterFrustumCullingVertices();
    
    @Environment(EnvType.CLIENT)
    void renderViewAreaMesh(Vec3d posInPlayerCoordinate, Consumer<Vec3d> vertexOutput);
}
