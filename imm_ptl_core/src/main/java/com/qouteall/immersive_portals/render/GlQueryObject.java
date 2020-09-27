package com.qouteall.immersive_portals.render;

import org.apache.commons.lang3.Validate;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

import java.util.ArrayList;

public class GlQueryObject {
    private int idQueryObject = -1;
    private boolean isQuerying = false;
    
    public GlQueryObject(int handle) {
        this.idQueryObject = handle;
    }
    
    public void performQueryAnySamplePassed(Runnable renderingFunc) {
        performQuery(renderingFunc, GL33.GL_ANY_SAMPLES_PASSED);
    }
    
    public void performQuerySampleNumPassed(Runnable renderingFunc) {
        performQuery(renderingFunc, GL15.GL_SAMPLES_PASSED);
    }
    
    private void performQuery(Runnable renderingFunc, int glQueryType) {
        Validate.isTrue(isValid());
        
        Validate.isTrue(!isQuerying);
        
        GL15.glBeginQuery(glQueryType, idQueryObject);
        
        isQuerying = true;
        
        renderingFunc.run();
        
        GL15.glEndQuery(glQueryType);
        
        isQuerying = false;
    }
    
    public boolean fetchQueryResult() {
        Validate.isTrue(isValid());
        
        int result = GL15.glGetQueryObjecti(idQueryObject, GL15.GL_QUERY_RESULT);
        
        return result != 0;
    }
    
    private void dispose() {
        if (idQueryObject != -1) {
            GL15.glDeleteQueries(idQueryObject);
            idQueryObject = -1;
        }
    }
    
    public boolean isValid() {
        return idQueryObject != -1;
    }
    
    private static final ArrayList<GlQueryObject> queryObjects = new ArrayList<>();
    
    private static void prepareQueryObjects() {
        int[] buf = new int[500];
        GL15.glGenQueries(buf);
        for (int id : buf) {
            queryObjects.add(new GlQueryObject(id));
        }
    }
    
    public static GlQueryObject acquireQueryObject() {
        if (queryObjects.isEmpty()) {
            prepareQueryObjects();
        }
        
        return queryObjects.remove(queryObjects.size() - 1);
    }
    
    public static void returnQueryObject(GlQueryObject obj){
        if (queryObjects.size() > 1500) {
            obj.dispose();
        }else {
            queryObjects.add(obj);
        }
    }
}
