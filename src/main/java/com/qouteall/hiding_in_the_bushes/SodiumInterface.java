package com.qouteall.hiding_in_the_bushes;

import com.qouteall.immersive_portals.chunk_loading.MyClientChunkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;

import java.util.function.BiFunction;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public class SodiumInterface {
    public static boolean isSodiumPresent = false;
    
    public static BiFunction<ClientWorld, Integer, ClientChunkManager> createClientChunkManager =
        MyClientChunkManager::new;
    
    public static Function<WorldRenderer, Object> createNewRenderingContext = (w) -> null;
    
    public static BiFunction<WorldRenderer, Object, Object> switchRenderingContext = (a, b) -> {
        return null;
    };
    
}
