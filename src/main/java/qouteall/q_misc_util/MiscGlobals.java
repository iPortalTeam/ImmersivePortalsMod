package qouteall.q_misc_util;

import net.minecraft.server.MinecraftServer;
import qouteall.q_misc_util.my_util.MyTaskList;

import java.lang.ref.WeakReference;

public class MiscGlobals {
    public static WeakReference<MinecraftServer> refMinecraftServer =
        new WeakReference<>(null);
    
    public static final MyTaskList serverTaskList = new MyTaskList();
    
}
