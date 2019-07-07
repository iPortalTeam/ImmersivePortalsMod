package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.portal_entity.PortalEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;

public class ViewAreaRenderer {
    static void buildPortalViewAreaTrianglesBuffer(
        Vec3d fogColor, PortalEntity portal, BufferBuilder bufferbuilder,
        Entity viewEntity
    ) {
        //if layerWidth is small, the teleportation will not be seamless
        
        //counter-clockwise triangles are front-faced in default
        
        final float layerWidth = 0.3F;
        
        bufferbuilder.begin(GL_TRIANGLES, VertexFormats.POSITION_COLOR);
        
        //these 4 vertices are shrink-ed
        Vec3d offset1 = portal.getPos().subtract(viewEntity.getPos());
        Vec3d[] frontFace =
            Arrays.stream(portal.getFourVertices(0))
            .map(pos->pos.add(offset1))
            .toArray(Vec3d[]::new);
        assert false;
        Vec3d[] frontFaceShrunken = portal.getFourVertices(0.01);
        Vec3d offset = portal.normal.multiply(-layerWidth);
        Vec3d[] backFace = {
            frontFaceShrunken[0].add(offset),
            frontFaceShrunken[1].add(offset),
            frontFaceShrunken[2].add(offset),
            frontFaceShrunken[3].add(offset)
        };
        
        //3  2
        //1  0
        
        //we do not render the front side
        
        //back side can only be seen from front
        putIntoQuad(
            bufferbuilder,
            backFace[0],
            backFace[2],
            backFace[3],
            backFace[1],
            fogColor
        );
        
        //right side can only be seen from center
        putIntoQuad(
            bufferbuilder,
            backFace[2],
            backFace[0],
            frontFace[0],
            frontFace[2],
            fogColor
        );
        
        //left side can only be seen from center
        putIntoQuad(
            bufferbuilder,
            backFace[1],
            backFace[3],
            frontFace[3],
            frontFace[1],
            fogColor
        );
        
        //top side can only be seen from center
        putIntoQuad(
            bufferbuilder,
            backFace[3],
            backFace[2],
            frontFace[2],
            frontFace[3],
            fogColor
        );
        
        //bottom side can only be seen from bottom
        putIntoQuad(
            bufferbuilder,
            backFace[0],
            backFace[1],
            frontFace[1],
            frontFace[0],
            fogColor
        );
    }
    
    static void putIntoVertex(BufferBuilder bufferBuilder, Vec3d pos, Vec3d fogColor) {
        bufferBuilder.vertex(pos.x, pos.y, pos.z).
            color((float) fogColor.x, (float) fogColor.y, (float) fogColor.z, 1).
            next();
    }
    
    //a d
    //b c
    private static void putIntoQuad(
        BufferBuilder bufferBuilder,
        Vec3d a,
        Vec3d b,
        Vec3d c,
        Vec3d d,
        Vec3d fogColor
    ) {
        //counter-clockwise triangles are front-faced in default
        
        putIntoVertex(bufferBuilder, b, fogColor);
        putIntoVertex(bufferBuilder, c, fogColor);
        putIntoVertex(bufferBuilder, d, fogColor);
        
        putIntoVertex(bufferBuilder, d, fogColor);
        putIntoVertex(bufferBuilder, a, fogColor);
        putIntoVertex(bufferBuilder, b, fogColor);
        
    }
}
