package qouteall.q_misc_util;

import java.util.HashSet;

public class LifecycleHack {
    private static final HashSet<String> stableNamespaces = new HashSet<>();
    
    public static void markNamespaceStable(String namespace) {
        stableNamespaces.add(namespace);
    }
    
    public static boolean isNamespaceStable(String namespace) {
        return stableNamespaces.contains(namespace);
    }
}
