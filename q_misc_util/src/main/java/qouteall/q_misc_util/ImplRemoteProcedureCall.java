package qouteall.q_misc_util;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ImplRemoteProcedureCall {
    public static final Gson gson;
    
    private static final ConcurrentHashMap<String, Method> methodCache = new ConcurrentHashMap<>();
    
    private static final ImmutableMap<Class, BiConsumer<FriendlyByteBuf, Object>> serializerMap;
    private static final ImmutableMap<Type, Function<FriendlyByteBuf, Object>> deserializerMap;
    
    private static final JsonParser jsonParser = new JsonParser();
    
    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gson = gsonBuilder.create();
        
        serializerMap = ImmutableMap.<Class, BiConsumer<FriendlyByteBuf, Object>>builder()
            .put(ResourceLocation.class, (buf, o) -> buf.writeResourceLocation(((ResourceLocation) o)))
            .put(ResourceKey.class, (buf, o) -> buf.writeResourceLocation(((ResourceKey) o).location()))
            .put(BlockPos.class, (buf, o) -> buf.writeBlockPos(((BlockPos) o)))
            .put(Vec3.class, (buf, o) -> {
                Vec3 vec = (Vec3) o;
                buf.writeDouble(vec.x);
                buf.writeDouble(vec.y);
                buf.writeDouble(vec.z);
            })
            .put(UUID.class, (buf, o) -> buf.writeUUID(((UUID) o)))
            .put(Block.class, (buf, o) -> serializeByCodec(buf, BuiltInRegistries.BLOCK.byNameCodec(), o))
            .put(Item.class, (buf, o) -> serializeByCodec(buf, BuiltInRegistries.ITEM.byNameCodec(), o))
            .put(BlockState.class, (buf, o) -> serializeByCodec(buf, BlockState.CODEC, o))
            .put(ItemStack.class, (buf, o) -> serializeByCodec(buf, ItemStack.CODEC, o))
            .put(CompoundTag.class, (buf, o) -> buf.writeNbt(((CompoundTag) o)))
            .put(Component.class, (buf, o) -> buf.writeComponent(((Component) o)))
            .put(byte[].class, (buf, o) -> buf.writeByteArray(((byte[]) o)))
            .build();
        
        deserializerMap = ImmutableMap.<Type, Function<FriendlyByteBuf, Object>>builder()
            .put(ResourceLocation.class, buf -> buf.readResourceLocation())
            .put(
                new TypeToken<ResourceKey<Level>>() {}.getType(),
                buf -> ResourceKey.create(
                    Registries.DIMENSION, buf.readResourceLocation()
                )
            )
            .put(
                new TypeToken<ResourceKey<Biome>>() {}.getType(),
                buf -> ResourceKey.create(
                    Registries.BIOME, buf.readResourceLocation()
                )
            )
            .put(BlockPos.class, buf -> buf.readBlockPos())
            .put(Vec3.class, buf ->
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble())
            )
            .put(UUID.class, buf -> buf.readUUID())
            .put(Block.class, buf -> deserializeByCodec(buf, BuiltInRegistries.BLOCK.byNameCodec()))
            .put(Item.class, buf -> deserializeByCodec(buf, BuiltInRegistries.ITEM.byNameCodec()))
            .put(BlockState.class, buf -> deserializeByCodec(buf, BlockState.CODEC))
            .put(ItemStack.class, buf -> deserializeByCodec(buf, ItemStack.CODEC))
            .put(CompoundTag.class, buf -> buf.readNbt())
            .put(Component.class, buf -> buf.readComponent())
            .put(byte[].class, buf -> buf.readByteArray())
            .build();
    }
    
    private static Object deserializeByCodec(FriendlyByteBuf buf, Codec codec) {
        String jsonString = readStringNonClientOnly(buf);
        JsonElement jsonElement = jsonParser.parse(jsonString);
        
        return codec.parse(JsonOps.INSTANCE, jsonElement).getOrThrow(
            false, e -> {throw new RuntimeException(e.toString());}
        );
    }
    
    // readString() is client only
    private static String readStringNonClientOnly(FriendlyByteBuf buf) {
        return buf.readUtf(32767);
    }
    
    private static Object deserialize(FriendlyByteBuf buf, Type type) {
        Function<FriendlyByteBuf, Object> deserializer = deserializerMap.get(type);
        if (deserializer == null) {
            String json = readStringNonClientOnly(buf);
            return gson.fromJson(json, type);
        }
        
        return deserializer.apply(buf);
    }
    
    private static void serialize(FriendlyByteBuf buf, Object object) {
        BiConsumer<FriendlyByteBuf, Object> serializer = serializerMap.get(object.getClass());
        
        if (serializer == null) {
            serializer = serializerMap.entrySet().stream().filter(
                e -> e.getKey().isAssignableFrom(object.getClass())
            ).findFirst().map(Map.Entry::getValue).orElse(null);
        }
        
        if (serializer == null) {
            String json = gson.toJson(object);
            buf.writeUtf(json);
            return;
        }
        
        serializer.accept(buf, object);
    }
    
    private static void serializeByCodec(FriendlyByteBuf buf, Codec codec, Object object) {
        JsonElement result = (JsonElement) codec.encodeStart(JsonOps.INSTANCE, object).getOrThrow(
            false, e -> {
                throw new RuntimeException(e.toString());
            }
        );
        
        String jsonString = gson.toJson(result);
        buf.writeUtf(jsonString);
    }
    
    @Environment(EnvType.CLIENT)
    public static ServerboundCustomPayloadPacket createC2SPacket(
        String methodPath,
        Object... arguments
    ) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        
        serializeStringWithArguments(methodPath, arguments, buf);
        
        return new ServerboundCustomPayloadPacket(MiscNetworking.id_ctsRemote, buf);
    }
    
    public static ClientboundCustomPayloadPacket createS2CPacket(
        String methodPath,
        Object... arguments
    ) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        
        serializeStringWithArguments(methodPath, arguments, buf);
        
        return new ClientboundCustomPayloadPacket(MiscNetworking.id_stcRemote, buf);
    }
    
    @Environment(EnvType.CLIENT)
    public static Runnable clientReadPacketAndGetHandler(FriendlyByteBuf buf) {
        String methodPath = readStringNonClientOnly(buf);
        
        Method method = getMethodByPath(methodPath);
        
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        
        Object[] arguments = new Object[genericParameterTypes.length];
        
        for (int i = 0; i < genericParameterTypes.length; i++) {
            Type parameterType = genericParameterTypes[i];
            Object obj = deserialize(buf, parameterType);
            arguments[i] = obj;
        }
        
        return () -> {
            try {
                method.invoke(null, arguments);
            }
            catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        };
    }
    
    public static Runnable serverReadPacketAndGetHandler(ServerPlayer player, FriendlyByteBuf buf) {
        String methodPath = readStringNonClientOnly(buf);
        
        Method method = getMethodByPath(methodPath);
        
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        
        Object[] arguments = new Object[genericParameterTypes.length];
        arguments[0] = player;
        
        //the first argument is the player
        for (int i = 1; i < genericParameterTypes.length; i++) {
            Type parameterType = genericParameterTypes[i];
            Object obj = deserialize(buf, parameterType);
            arguments[i] = obj;
        }
        
        return () -> {
            try {
                method.invoke(null, arguments);
            }
            catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        };
    }
    
    private static void serializeStringWithArguments(
        String methodPath, Object[] arguments, FriendlyByteBuf buf
    ) {
        buf.writeUtf(methodPath);
        
        for (Object argument : arguments) {
            serialize(buf, argument);
        }
    }
    
    private static Method getMethodByPath(String methodPath) {
        Method result = methodCache.get(methodPath);
        if (result != null) {
            return result;
        }
        
        //because it may throw exception, does not use computeIfAbsent
        Method method = findMethodByPath(methodPath);
        Validate.notNull(method);
        
        methodCache.put(methodPath, method);
        return method;
    }
    
    private static Method findMethodByPath(String methodPath) {
        int lastDotIndex = methodPath.lastIndexOf('.');
        
        Validate.isTrue(lastDotIndex != -1);
        String classPath = methodPath.substring(0, lastDotIndex);
        String methodName = methodPath.substring(lastDotIndex + 1);
        
        if (!classPath.contains("RemoteCallable")) {
            throw new RuntimeException("The class path must contain \"RemoteCallable\"");
        }
        
        Class<?> aClass;
        try {
            aClass = Class.forName(classPath);
        }
        catch (ClassNotFoundException e) {
            int dotIndex = classPath.lastIndexOf('.');
            if (dotIndex != -1) {
                String newClassPath =
                    classPath.substring(0, dotIndex) + "$" + classPath.substring(dotIndex + 1);
                try {
                    aClass = Class.forName(newClassPath);
                }
                catch (ClassNotFoundException e1) {
                    throw new RuntimeException("Cannot find class " + classPath, e);
                }
            }
            else {
                throw new RuntimeException("Cannot find class " + classPath, e);
            }
        }
        
        Method method = Arrays.stream(aClass.getMethods()).filter(
            m -> m.getName().equals(methodName)
        ).findFirst().orElseThrow(() -> new RuntimeException(
            "Cannot find method " + methodPath + " . If it's a private method, make it public."
        ));
        
        return method;
    }
    
}
