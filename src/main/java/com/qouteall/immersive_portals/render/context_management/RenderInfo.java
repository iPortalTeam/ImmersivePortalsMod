package com.qouteall.immersive_portals.render.context_management;

import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;

public class RenderInfo {
    public ClientWorld world;
    public Vec3d cameraPos;
    public Matrix4f additionalTransformation;
    @Nullable
    public Portal portal;
    
    public RenderInfo(
        ClientWorld world, Vec3d cameraPos,
        Matrix4f additionalTransformation, @Nullable Portal portal
    ) {
        this.world = world;
        this.cameraPos = cameraPos;
        this.additionalTransformation = additionalTransformation;
        this.portal = portal;
    }
}
