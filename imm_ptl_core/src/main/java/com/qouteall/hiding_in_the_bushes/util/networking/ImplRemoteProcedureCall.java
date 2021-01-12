package com.qouteall.hiding_in_the_bushes.util.networking;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.serialization.Codec;
import com.qouteall.hiding_in_the_bushes.MyNetwork;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ImplRemoteProcedureCall {
    public static final Gson gson;
    
    private static final ConcurrentHashMap<String, Method> methodCache = new ConcurrentHashMap<>();
    
    private static final ImmutableMap<Class, BiConsumer<PacketByteBuf, Object>> serializerMap;
    private static final ImmutableMap<Type, Function<PacketByteBuf, Object>> deserializerMap;
    
    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gson = gsonBuilder.create();
        
        serializerMap = ImmutableMap.<Class, BiConsumer<PacketByteBuf, Object>>builder()
            .put(Identifier.class, (buf, o) -> buf.writeIdentifier(((Identifier) o)))
            .put(RegistryKey.class, (buf, o) -> buf.writeIdentifier(((RegistryKey) o).getValue()))
            .put(BlockPos.class, (buf, o) -> buf.writeBlockPos(((BlockPos) o)))
            .put(Vec3d.class, (buf, o) -> {
                Vec3d vec = (Vec3d) o;
                buf.writeDouble(vec.x);
                buf.writeDouble(vec.y);
                buf.writeDouble(vec.z);
            })
            .put(UUID.class, (buf, o) -> buf.writeUuid(((UUID) o)))
            .put(Block.class, (buf, o) -> serialize(buf, Registry.BLOCK))
            .put(Item.class, (buf, o) -> serialize(buf, Registry.ITEM))
            .put(BlockState.class, (buf, o) -> serialize(buf, BlockState.CODEC))
            .put(ItemStack.class, (buf, o) -> serialize(buf, ItemStack.CODEC))
            .put(CompoundTag.class, (buf, o) -> buf.writeCompoundTag(((CompoundTag) o)))
            .put(Text.class, (buf, o) -> buf.writeText(((Text) o)))
            .build();
        
        deserializerMap = ImmutableMap.<Type, Function<PacketByteBuf, Object>>builder()
            .put(Identifier.class, buf -> buf.readIdentifier())
            .put(
                new TypeToken<RegistryKey<World>>() {}.getType(),
                buf -> RegistryKey.of(
                    Registry.DIMENSION, buf.readIdentifier()
                )
            )
            .put(
                new TypeToken<RegistryKey<Biome>>() {}.getType(),
                buf -> RegistryKey.of(
                    Registry.BIOME_KEY, buf.readIdentifier()
                )
            )
            .put(BlockPos.class, buf -> buf.readBlockPos())
            .put(Vec3d.class, buf ->
                new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble())
            )
            .put(UUID.class, buf -> buf.readUuid())
            .put(Block.class, buf -> deserializeByCodec(buf, Registry.BLOCK))
            .put(Item.class, buf -> deserializeByCodec(buf, Registry.ITEM))
            .put(BlockState.class, buf -> deserializeByCodec(buf, BlockState.CODEC))
            .put(ItemStack.class, buf -> deserializeByCodec(buf, ItemStack.CODEC))
            .put(CompoundTag.class, buf -> buf.readCompoundTag())
            .put(Text.class, buf -> buf.readText())
            .build();
    }
    
    private static Object deserializeByCodec(PacketByteBuf buf, Codec codec) {
        try {
            return buf.decode(codec);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static Object deserialize(PacketByteBuf buf, Type type) {
        Function<PacketByteBuf, Object> deserializer = deserializerMap.get(type);
        if (deserializer == null) {
            String json = buf.readString();
            return gson.fromJson(json, type);
        }
        
        return deserializer.apply(buf);
    }
    
    private static void serialize(PacketByteBuf buf, Object object) {
        BiConsumer<PacketByteBuf, Object> serializer = serializerMap.get(object.getClass());
        if (serializer == null) {
            String json = gson.toJson(object);
            buf.writeString(json);
            return;
        }
        
        serializer.accept(buf, object);
    }
    
    private static void serializeByCodec(PacketByteBuf buf, Codec codec, Object object) {
        try {
            buf.encode(codec, object);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Environment(EnvType.CLIENT)
    public static CustomPayloadC2SPacket createC2SPacket(
        String methodPath,
        Object... arguments
    ) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        
        serializeStringWithArguments(methodPath, arguments, buf);
        
        return new CustomPayloadC2SPacket(MyNetwork.id_ctsRemote, buf);
    }
    
    public static CustomPayloadS2CPacket createS2CPacket(
        String methodPath,
        Object... arguments
    ) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        
        serializeStringWithArguments(methodPath, arguments, buf);
        
        return new CustomPayloadS2CPacket(MyNetwork.id_stcRemote, buf);
    }
    
    @Environment(EnvType.CLIENT)
    public static void clientHandlePacket(PacketByteBuf buf) {
        String methodPath = buf.readString();
        
        Method method = getMethodByPath(methodPath);
        
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        
        Object[] arguments = new Object[genericParameterTypes.length];
        
        for (int i = 0; i < genericParameterTypes.length; i++) {
            Type parameterType = genericParameterTypes[i];
            String str = buf.readString();
            Object obj = deserializeObjectFromJson(parameterType, str);
            arguments[i] = obj;
        }
        
        try {
            method.invoke(null, arguments);
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static Object deserializeObjectFromJson(Type parameterType, String str) {
        return gson.fromJson(str, parameterType);
    }
    
    public static void serverHandlePacket(ServerPlayerEntity player, PacketByteBuf buf) {
        String methodPath = buf.readString();
        
        Method method = getMethodByPath(methodPath);
        
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        
        Object[] arguments = new Object[genericParameterTypes.length + 1];
        arguments[0] = player;
        
        for (int i = 0; i < genericParameterTypes.length; i++) {
            Type parameterType = genericParameterTypes[i];
            String str = buf.readString();
            Object obj = deserializeObjectFromJson(parameterType, str);
            arguments[i + 1] = obj;
        }
        
        try {
            method.invoke(null, arguments);
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static void serializeStringWithArguments(
        String methodPath, Object[] arguments, PacketByteBuf buf
    ) {
        buf.writeString(methodPath);
        
        Object[] argumentArray = arguments;
        
        for (Object obj : argumentArray) {
            String jsonInfo = gson.toJson(obj);
            buf.writeString(jsonInfo);
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
            throw new RuntimeException("Cannot find class " + classPath, e);
        }
        
        Method method = Arrays.stream(aClass.getMethods()).filter(
            m -> m.getName().equals(methodName)
        ).findFirst().orElseThrow(() -> new RuntimeException(
            "Cannot find method " + methodPath
        ));
        
        return method;
    }
    
}
