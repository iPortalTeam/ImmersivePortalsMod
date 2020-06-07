package com.qouteall.immersive_portals;

import com.google.common.collect.Streams;
import com.qouteall.immersive_portals.ducks.IEClientWorld;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import com.qouteall.immersive_portals.render.context_management.PortalRendering;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.LiteralText;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;
import java.util.List;
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
    
    //NOTE this may not be reliable
    public static DimensionType getOriginalDimension() {
        if (PortalRendering.isRendering()) {
            return RenderStates.originalPlayerDimension;
        }
        else {
            return MinecraftClient.getInstance().player.dimension;
        }
    }
    
    public static boolean shouldDisableFog() {
        return OFInterface.shouldDisableFog.getAsBoolean();
    }
    
    //do not inline this
    //or it will crash in server
    public static World getClientWorld(RegistryKey<World> dimension) {
        return CGlobal.clientWorldLoader.getWorld(dimension);
    }
    
    public static List<GlobalTrackedPortal> getClientGlobalPortal(World world) {
        return ((IEClientWorld) world).getGlobalPortals();
    }
    
    public static Stream<Portal> getClientNearbyPortals(double range) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        List<GlobalTrackedPortal> globalPortals = ((IEClientWorld) player.world).getGlobalPortals();
        List<Portal> nearbyPortals = McHelper.findEntitiesRough(
            Portal.class,
            player.world,
            player.getPos(),
            (int)(range / 16),
            p -> true
        );
        if (globalPortals == null) {
            return nearbyPortals.stream();
        }
        else {
            return Streams.concat(
                globalPortals.stream().filter(
                    p -> p.getDistanceToNearestPointInPortal(player.getPos()) < range * 2
                ),
                nearbyPortals.stream()
            );
        }
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
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
            new LiteralText(str)
        );
    }
    
    public static class Rect {
        public float xMin;
        public float yMin;
        public float xMax;
        public float yMax;
        
        public Rect(float xMin, float yMin, float xMax, float yMax) {
            this.xMin = xMin;
            this.yMin = yMin;
            this.xMax = xMax;
            this.yMax = yMax;
        }
        
        public void grow(float delta) {
            xMin -= delta;
            xMax += delta;
            yMin -= delta;
            yMax += delta;
        }
        
        public static Rect of(Screen screen) {
            return new Rect(
                0, 0,
                screen.width, screen.height
            );
        }

//        public static Rect of(AbstractButtonWidget widget) {
//            return new Rect(
//                widget.x,widget.y,
//                widget.x+widget.getWidth(),
//                widget.y+widget.
//            )
//        }
    }
    
    public static interface LayoutFunc {
        public void apply(int from, int to);
    }
    
    public static class LayoutElement {
        public boolean fixedLength;
        //if fixed, this length. if not fixed, this is weight
        public int length;
        public LayoutFunc apply;
        
        public LayoutElement(boolean fixedLength, int length, LayoutFunc apply) {
            this.fixedLength = fixedLength;
            this.length = length;
            this.apply = apply;
        }
        
        public static LayoutElement blankSpace(int length) {
            return new LayoutElement(true, length, (a, b) -> {
            });
        }
        
        public static LayoutElement layoutX(ButtonWidget widget, int widthRatio) {
            return new LayoutElement(
                false,
                widthRatio,
                (a, b) -> {
                    widget.x = a;
                    widget.setWidth(b - a);
                }
            );
        }
        
        public static LayoutElement layoutY(ButtonWidget widget, int height) {
            return new LayoutElement(
                true,
                height,
                (a, b) -> {
                    widget.y = a;
                }
            );
        }
    }
    
    public static void layout(
        int from, int to,
        LayoutElement... elements
    ) {
        int totalEscalateWeight = Arrays.stream(elements)
            .filter(e -> !e.fixedLength)
            .mapToInt(e -> e.length)
            .sum();
        
        int totalFixedLen = Arrays.stream(elements)
            .filter(e -> e.fixedLength)
            .mapToInt(e -> e.length)
            .sum();
        
        int totalEscalateLen = (to - from - totalFixedLen);
        
        int currCoordinate = from;
        for (LayoutElement element : elements) {
            int currLen;
            if (element.fixedLength) {
                currLen = element.length;
            }
            else {
                currLen = element.length * totalEscalateLen / totalEscalateWeight;
            }
            element.apply.apply(currCoordinate, currCoordinate + currLen);
            currCoordinate += currLen;
        }
    }
    
}
