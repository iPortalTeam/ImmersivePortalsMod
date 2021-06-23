package qouteall.imm_ptl.core.iris_compatibility;

import net.coderbot.iris.Iris;

public class IrisInterface {
    
    public static class Invoker {
        public boolean isIrisPresent() {
            return false;
        }
        
        public boolean isShaders() {
            return false;
        }
    }
    
    public static class OnIrisPresent extends Invoker {
        
        @Override
        public boolean isIrisPresent() {
            return true;
        }
        
        @Override
        public boolean isShaders() {
            return Iris.getCurrentPack().isPresent();
        }
    }
    
    public static Invoker invoker = new Invoker();
}
