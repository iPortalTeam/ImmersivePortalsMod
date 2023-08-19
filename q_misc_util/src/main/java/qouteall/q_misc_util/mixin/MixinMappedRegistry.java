package qouteall.q_misc_util.mixin;

import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import qouteall.q_misc_util.ducks.IEMappedRegistry2;

import java.util.List;
import java.util.Map;

@Mixin(MappedRegistry.class)
public abstract class MixinMappedRegistry<T> implements IEMappedRegistry2 {
    @Shadow
    @Final
    private Map<ResourceLocation, Holder.Reference<T>> byLocation;
    
    @Shadow
    @Final
    private static Logger LOGGER;
    
    @Shadow
    public abstract @Nullable T byId(int id);
    
    @Shadow
    @Final
    private ObjectList<Holder.Reference<T>> byId;
    
    @Shadow
    @Final
    private Object2IntMap<T> toId;
    
    @Shadow
    @Final
    private Map<ResourceKey<T>, Holder.Reference<T>> byKey;
    
    @Shadow
    @Final
    private ResourceKey<? extends Registry<T>> key;
    
    @Shadow
    @Final
    private Map<T, Holder.Reference<T>> byValue;
    
    @Shadow
    @Final
    private Map<T, Lifecycle> lifecycles;
    
    @Shadow
    private @Nullable List<Holder.Reference<T>> holdersInOrder;
    
    @Override
    public boolean ip_forceRemove(ResourceLocation id) {
        Holder.Reference<T> holder = byLocation.remove(id);
        
        if (holder == null) {
            return false;
        }
        
        T value = holder.value();
        
        if (value == null) {
            LOGGER.error("[ImmPtl] missing value in holder {}", holder);
            return false;
        }
        
        int intId = toId.getInt(value);
        
        if (intId == -1) {
            LOGGER.error("[ImmPtl] missing integer id for {}", value);
        }
        else {
            toId.removeInt(value);
            byId.set(intId, null);
        }
        
        byKey.remove(ResourceKey.create(key, id));
        byValue.remove(value);
        lifecycles.remove(value);
        
        holdersInOrder = null;
        
        return true;
    }
    
}
