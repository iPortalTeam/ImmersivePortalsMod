package qouteall.q_misc_util;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qouteall.q_misc_util.ducks.IEMinecraftServer_Misc;
import qouteall.q_misc_util.mixin.IELevelStorageAccess_Misc;

import java.lang.reflect.Type;
import java.nio.file.Path;

public class MiscHelper {
    
    private static final Logger LOGGER = LogManager.getLogger();
    
    public static final Gson gson;
    
    private static class DimensionIDJsonAdapter
        implements JsonSerializer<ResourceKey<Level>>, JsonDeserializer<ResourceKey<Level>> {
        
        @Override
        public ResourceKey<Level> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String str = json.getAsString();
            return Helper.dimIdToKey(str);
        }
        
        @Override
        public JsonElement serialize(ResourceKey<Level> src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.location().toString());
        }
    }
    
    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();
        
        gsonBuilder.registerTypeAdapter(
            new TypeToken<ResourceKey<Level>>() {}.getType(),
            new DimensionIDJsonAdapter()
        );
        
        gson = gsonBuilder.create();
    }
    
    /**
     * {@link ReentrantBlockableEventLoop#scheduleExecutables()}
     * The execution may get deferred on the render thread
     */
    @Environment(EnvType.CLIENT)
    public static void executeOnRenderThread(Runnable runnable) {
        Minecraft client = Minecraft.getInstance();
        
        if (client.isSameThread()) {
            try {
                runnable.run();
            }
            catch (Exception e) {
                LOGGER.error("Processing task on render thread", e);
            }
        }
        else {
            client.execute(runnable);
        }
    }
    
    /**
     * TODO support multi-server-in-one-JVM
     */
    @Deprecated
    public static MinecraftServer getServer() {
        return MiscGlobals.refMinecraftServer.get();
    }
    
    public static void executeOnServerThread(MinecraftServer server, Runnable runnable) {
        if (server.isSameThread()) {
            try {
                runnable.run();
            }
            catch (Exception e) {
                LOGGER.error("Processing task on server thread", e);
            }
        }
        else {
            server.execute(runnable);
        }
    }
    
    public static boolean isDedicatedServer() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
    }
    
    
    public static Path getWorldSavingDirectory(MinecraftServer server) {
        Validate.notNull(server);
        Path saveDir =
            ((IELevelStorageAccess_Misc) ((IEMinecraftServer_Misc) server).ip_getStorageSource())
                .ip_getLevelPath().path();
        return saveDir;
    }
}
