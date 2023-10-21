package qouteall.imm_ptl.core.portal.shape;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class PortalShapeSerialization {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static record Serializer<T extends PortalShape>(
        String typeName,
        Class<T> clazz,
        Function<T, CompoundTag> serializer,
        Function<CompoundTag, T> deserializer
    ) {}
    
    private static final Map<String, Serializer<? extends PortalShape>>
        FROM_TYPE_NAME = new HashMap<>();
    
    private static final Map<Class<?>, Serializer<? extends PortalShape>>
        FROM_CLASS = new HashMap<>();
    
    public static void addSerializer(Serializer<? extends PortalShape> serializer) {
        FROM_TYPE_NAME.put(serializer.typeName(), serializer);
        FROM_CLASS.put(serializer.clazz(), serializer);
    }
    
    public static @Nullable PortalShape deserialize(CompoundTag tag) {
        String typeName = tag.getString("type");
        Serializer<? extends PortalShape> serializer = FROM_TYPE_NAME.get(typeName);
        if (serializer == null) {
            LOGGER.warn("unknown portal shape type {} {}", typeName, tag);
            return null;
        }
        return serializer.deserializer().apply(tag);
    }
    
    @SuppressWarnings("unchecked")
    public static CompoundTag serialize(PortalShape portalShape) {
        Serializer<? extends PortalShape> serializer = FROM_CLASS.get(portalShape.getClass());
        
        if (serializer == null) {
            LOGGER.warn("unknown portal shape class {}", portalShape.getClass());
            return new CompoundTag();
        }
        
        CompoundTag tag = ((Function<PortalShape, CompoundTag>) serializer.serializer())
            .apply(portalShape);
        tag.putString("type", serializer.typeName());
        
        return tag;
    }
}
