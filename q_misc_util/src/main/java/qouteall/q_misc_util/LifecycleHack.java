package qouteall.q_misc_util;

public class LifecycleHack {
    
    public static void markNamespaceStable(String namespace) {
        MiscGlobals.stableNamespaces.add(namespace);
    }
}
