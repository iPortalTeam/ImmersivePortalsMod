package qouteall.imm_ptl.core;

import qouteall.imm_ptl.core.ducks.IEClientWorld;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmChatLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.lwjgl.opengl.GL11.GL_NO_ERROR;

@Environment(EnvType.CLIENT)
public class CHelper {
    
    private static int reportedErrorNum = 0;
    
    public static PlayerListEntry getClientPlayerListEntry() {
        return MinecraftClient.getInstance().getNetworkHandler().getPlayerListEntry(
            MinecraftClient.getInstance().player.getGameProfile().getId()
        );
    }
    
    public static boolean shouldDisableFog() {
        return OFInterface.shouldDisableFog.getAsBoolean();
    }
    
    //do not inline this
    //or it will crash in server
    public static World getClientWorld(RegistryKey<World> dimension) {
        return ClientWorldLoader.getWorld(dimension);
    }
    
    public static List<Portal> getClientGlobalPortal(World world) {
        if (world instanceof ClientWorld) {
            return ((IEClientWorld) world).getGlobalPortals();
        }
        else {
            return null;
        }
    }
    
    public static Stream<Portal> getClientNearbyPortals(double range) {
        return McHelper.getNearbyPortals(MinecraftClient.getInstance().player, range);
    }
    
    public static void checkGlError() {
        if (!Global.doCheckGlError) {
            return;
        }
        if (reportedErrorNum > 100) {
            return;
        }
        int errorCode = GL11.glGetError();
        if (errorCode != GL_NO_ERROR) {
            Helper.err("OpenGL Error" + errorCode);
            new Throwable().printStackTrace();
            reportedErrorNum++;
        }
    }
    
    public static void printChat(String str) {
        printChat(new LiteralText(str));
    }
    
    public static void printChat(LiteralText text) {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);
    }
    
    public static void openLinkConfirmScreen(
        Screen parent,
        String link
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.openScreen(new ConfirmChatLinkScreen(
            (result) -> {
                if (result) {
                    try {
                        Util.getOperatingSystem().open(new URI(link));
                    }
                    catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
                client.openScreen(parent);
            },
            link, true
        ));
    }
    
    public static Vec3d getCurrentCameraPos() {
        return MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
    }
    
    public static <T> T withWorldSwitched(Entity entity, Portal portal, Supplier<T> func) {
        
        World oldWorld = entity.world;
        Vec3d eyePos = McHelper.getEyePos(entity);
        Vec3d lastTickEyePos = McHelper.getLastTickEyePos(entity);
        
        entity.world = portal.getDestinationWorld();
        McHelper.setEyePos(
            entity,
            portal.transformPoint(eyePos),
            portal.transformPoint(lastTickEyePos)
        );
        
        try {
            T result = func.get();
            return result;
        }
        finally {
            entity.world = oldWorld;
            McHelper.setEyePos(entity, eyePos, lastTickEyePos);
        }
    }
    
    public static Iterable<Entity> getWorldEntityList(World world) {
        if (!(world instanceof ClientWorld)) {
            return (Iterable<Entity>) Collections.emptyList().iterator();
        }
        
        ClientWorld clientWorld = (ClientWorld) world;
        return clientWorld.getEntities();
    }
    
    /**
     * {@link ReentrantThreadExecutor#shouldExecuteAsync()}
     * The execution may get deferred on the render thread
     */
    public static void executeOnRenderThread(Runnable runnable) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client.isOnThread()) {
            runnable.run();
        }
        else {
            client.execute(runnable);
        }
    }
    
    public static double getSmoothCycles(long unitTicks) {
        int playerAge = MinecraftClient.getInstance().player.age;
        return (playerAge % unitTicks + RenderStates.tickDelta) / (double) unitTicks;
    }
}
