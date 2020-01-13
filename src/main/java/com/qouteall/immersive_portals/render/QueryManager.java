package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.CHelper;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

public class QueryManager {
    public static boolean isQuerying = false;
    private static int idQueryObject = -1;
    
    public static boolean renderAndGetDoesAnySamplePassed(Runnable renderingFunc) {
        if (idQueryObject == -1) {
            idQueryObject = GL15.glGenQueries();
            CHelper.checkGlError();
        }
        
        //mac does not support GL_ANY_SAMPLES_PASSED
        if (MinecraftClient.IS_SYSTEM_MAC) {
            return renderAndGetSampleCountPassed(renderingFunc) > 0;
        }
        
        assert (!isQuerying);
        
        GL15.glBeginQuery(GL33.GL_ANY_SAMPLES_PASSED, QueryManager.idQueryObject);
        
        isQuerying = true;
        
        renderingFunc.run();
        
        GL15.glEndQuery(GL33.GL_ANY_SAMPLES_PASSED);
        
        isQuerying = false;
        
        int result = GL15.glGetQueryObjecti(QueryManager.idQueryObject, GL15.GL_QUERY_RESULT);
        
        return result != 0;
    }
    
    public static int renderAndGetSampleCountPassed(Runnable renderingFunc) {
        assert (!isQuerying);
        
        if (idQueryObject == -1) {
            idQueryObject = GL15.glGenQueries();
            CHelper.checkGlError();
        }
        
        GL15.glBeginQuery(GL15.GL_SAMPLES_PASSED, QueryManager.idQueryObject);
        
        isQuerying = true;
        
        renderingFunc.run();
        
        GL15.glEndQuery(GL15.GL_SAMPLES_PASSED);
        
        isQuerying = false;
        
        int result = GL15.glGetQueryObjecti(QueryManager.idQueryObject, GL15.GL_QUERY_RESULT);
        
        return result;
    }
}
