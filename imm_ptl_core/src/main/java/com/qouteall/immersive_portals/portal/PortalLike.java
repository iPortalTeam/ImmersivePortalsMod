package com.qouteall.immersive_portals.portal;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public interface PortalLike {
    boolean isConventionalPortal();
    
    // bounding box
    Box getAreaBox();
    
    Vec3d getOriginPos();
    
    Vec3d getDestPos();
    
    World getOriginWorld();
    
    World getDestWorld();
    
    boolean isRoughlyVisibleTo(Vec3d cameraPos);
    
    Vec3d getContentDirection();
    
    @Nullable
    Quaternion getRotation();
    
    double getScale();
    
    boolean getIsMirror();
    
    boolean getIsGlobal();
    
    // used for advanced frustum culling
    @Nullable
    Vec3d[] getAggressiveAreaVertices();
    
    // used for super advanced frustum culling
    @Nullable
    Vec3d[] getConservativeAreaVertices();
    
    void renderViewAreaMesh(Vec3d posInPlayerCoordinate, Consumer<Vec3d> vertexOutput);
}
