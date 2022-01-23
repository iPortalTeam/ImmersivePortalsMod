package qouteall.imm_ptl.core.render;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;
import qouteall.imm_ptl.core.CHelper;

public class QueryManager {
    public static int queryStallCounter = 0;
    
    public static boolean isQuerying = false;
    private static int idQueryObject = -1;
    
    public static boolean renderAndGetDoesAnySamplePass(Runnable renderingFunc) {
        if (idQueryObject == -1) {
            idQueryObject = GL15.glGenQueries();
            CHelper.checkGlError();
        }
        
        //mac does not support GL_ANY_SAMPLES_PASSED
        if (Minecraft.ON_OSX) {
            return renderAndGetSampleCountPassed(renderingFunc) > 0;
        }
        
        assert (!isQuerying);
        
        GL15.glBeginQuery(GL33.GL_ANY_SAMPLES_PASSED, QueryManager.idQueryObject);
        
        isQuerying = true;
        
        renderingFunc.run();
        
        GL15.glEndQuery(GL33.GL_ANY_SAMPLES_PASSED);
        
        isQuerying = false;
        
        int result = GL15.glGetQueryObjecti(QueryManager.idQueryObject, GL15.GL_QUERY_RESULT);
        queryStallCounter++;
        
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
        queryStallCounter++;
        
        return result;
    }
}
