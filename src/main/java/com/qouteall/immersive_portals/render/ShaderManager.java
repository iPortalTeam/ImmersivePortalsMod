package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.Globals;
import com.qouteall.immersive_portals.my_util.Helper;

import com.qouteall.immersive_portals.portal_entity.PortalEntity;

import net.minecraft.client.MinecraftClient;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.io.IOUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class ShaderManager {
    private int idContentShaderProgram = -1;
    public static boolean isShaderEnabled = true;
    
    public ShaderManager() {
        idContentShaderProgram = compileShaderPrograms(
            "immersive_portals:shaders/content_shader_vs.glsl",
            "immersive_portals:shaders/content_shader_fs.glsl"
        );
    }
    
    //return the shader id
    //shaderType:could be GL_VERTEX_SHADER
    private int compileShader(String resourceName, int shaderType) {
        
        try {
            InputStream inputStream =
                MinecraftClient.getInstance().getResourceManager().getResource(
                    new Identifier(resourceName)
                ).getInputStream();
            
            String shaderCode = IOUtils.toString(inputStream, Charset.defaultCharset());
            
            int idShader = GL20.glCreateShader(shaderType);
            GL20.glShaderSource(idShader, shaderCode);
            GL20.glCompileShader(idShader);
            
            //check compiling errors
            int logLength = GL20.glGetShaderi(idShader, GL20.GL_INFO_LOG_LENGTH);
            if (logLength > 0) {
                String errorLog = GL20.glGetShaderInfoLog(idShader, logLength);
                Helper.err("SHADER COMPILE ERROR");
                System.err.println(errorLog);
            }
            
            return idShader;
            
            
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        
        
        return -1;
    }
    
    private int compileShaderPrograms(
        String vertexShaderName,
        String fragmentShaderName
    ) {
        int idVertexShader = compileShader(vertexShaderName, GL20.GL_VERTEX_SHADER);
        int idFragmentShader = compileShader(fragmentShaderName, GL20.GL_FRAGMENT_SHADER);
        
        int idProgram = GL20.glCreateProgram();
        GL20.glAttachShader(idProgram, idVertexShader);
        GL20.glAttachShader(idProgram, idFragmentShader);
        GL20.glLinkProgram(idProgram);
        
        //check errors
        int logLength = GL20.glGetProgrami(idProgram, GL20.GL_INFO_LOG_LENGTH);
        if (logLength > 0) {
            Helper.err("LINKING ERROR");
        }
        
        //is it ok?
        GL20.glDeleteShader(idVertexShader);
        GL20.glDeleteShader(idFragmentShader);
        
        return idProgram;
    }
    
    public void loadContentShaderAndShaderVars(Vec3d basePos) {
        if (!isShaderEnabled) {
            return;
        }
        
        GL20.glUseProgram(idContentShaderProgram);
        
        int uniModelView = GL20.glGetUniformLocation(idContentShaderProgram, "modelView");
        int uniProjection = GL20.glGetUniformLocation(idContentShaderProgram, "projection");
        int uniTextureMatrix = GL20.glGetUniformLocation(idContentShaderProgram, "textureMatrix");
        int uniPortalCenter = GL20.glGetUniformLocation(idContentShaderProgram, "portalCenter");
        int uniPortalNormal = GL20.glGetUniformLocation(idContentShaderProgram, "portalNormal");
        int uniPosBase = GL20.glGetUniformLocation(idContentShaderProgram, "posBase");
        int uniSampler = GL20.glGetUniformLocation(idContentShaderProgram, "sampler");
        int uniSampler2 = GL20.glGetUniformLocation(idContentShaderProgram, "sampler2");
        int uniFogStart = GL20.glGetUniformLocation(idContentShaderProgram, "fogStart");
        int uniFogEnd = GL20.glGetUniformLocation(idContentShaderProgram, "fogEnd");
        int uniFogColor = GL20.glGetUniformLocation(idContentShaderProgram, "fogColor");
        
        GL20.glUniformMatrix4fv(uniModelView, false, Helper.getModelViewMatrix());
        GL20.glUniformMatrix4fv(uniProjection, false, Helper.getProjectionMatrix());
        GL20.glUniformMatrix4fv(uniTextureMatrix, true, Helper.getTextureMatrix());
        
        PortalEntity portal = Globals.portalRenderManager.getRenderingPortalData();
        if (portal != null) {
            Vec3d cullingPoint = portal.getCullingPoint();
            GL20.glUniform3f(
                uniPortalCenter,
                (float) cullingPoint.x,
                (float) cullingPoint.y,
                (float) cullingPoint.z
            );
            GL20.glUniform3f(
                uniPortalNormal,
                (float) portal.getNormal().x,
                (float) portal.getNormal().y,
                (float) portal.getNormal().z
            );
        }
        else {
            Helper.err("NULL PORTAL");
        }
        
        GL20.glUniform3f(
            uniPosBase,
            (float) basePos.x,
            (float) basePos.y,
            (float) basePos.z
        );
        
        GL20.glUniform1i(uniSampler, 0);
        GL20.glUniform1i(uniSampler2, 1);
        
        GL20.glUniform1f(uniFogStart, getFogStart());
        GL20.glUniform1f(uniFogEnd, getFogEnd());
        float[] fogColor = getFogColor();
        GL20.glUniform3f(uniFogColor, fogColor[0], fogColor[1], fogColor[2]);
    }
    
    public void unloadShader() {
        GL20.glUseProgram(0);
    }
    
    private float getFogStart() {
        return GL11.glGetFloat(GL11.GL_FOG_START);
    }
    
    private float getFogEnd() {
        return GL11.glGetFloat(GL11.GL_FOG_END);
    }
    
    private float[] getFogColor() {
        float[] arr = new float[3];
        GL11.glGetFloatv(GL11.GL_FOG_COLOR, arr);
        return arr;
    }
}
