package qouteall.imm_ptl.core.portal.custom_portal_gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import qouteall.imm_ptl.core.McHelper;

import java.util.function.Function;

public abstract class PortalGenTrigger {
    public static final Codec<PortalGenTrigger> triggerCodec;
    
    public static final Registry<MapCodec<? extends PortalGenTrigger>> codecRegistry;
    
    
    public abstract MapCodec<? extends PortalGenTrigger> getCodec();
    
    public static class UseItemTrigger extends PortalGenTrigger {
        public final Item item;
        public final boolean consume;
        
        public UseItemTrigger(Item item, boolean consume) {
            this.item = item;
            this.consume = consume;
        }
        
        public boolean shouldConsume(UseOnContext context) {
            if (!consume) {
                return false;
            }
            
            Player player = context.getPlayer();
            if (player != null) {
                if (player.isCreative()) {
                    return false;
                }
            }
            
            return true;
        }
        
        @Override
        public MapCodec<? extends PortalGenTrigger> getCodec() {
            return useItemTriggerCodec;
        }
    }
    
    public static class ThrowItemTrigger extends PortalGenTrigger {
        public final Item item;
        
        public ThrowItemTrigger(Item item) {
            this.item = item;
        }
        
        @Override
        public MapCodec<? extends PortalGenTrigger> getCodec() {
            return throwItemTriggerCodec;
        }
    }
    
    public static class ConventionalDimensionChangeTrigger extends PortalGenTrigger {
        
        public ConventionalDimensionChangeTrigger() {}
        
        @Override
        public MapCodec<? extends PortalGenTrigger> getCodec() {
            return conventionalDimensionChangeCodec;
        }
    }
    
    public static final MapCodec<UseItemTrigger> useItemTriggerCodec = RecordCodecBuilder.mapCodec(instance -> {
        return instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(o -> o.item),
            Codec.BOOL.optionalFieldOf("consume", false).forGetter(o -> o.consume)
        ).apply(instance, instance.stable(UseItemTrigger::new));
    });
    
    public static final MapCodec<ThrowItemTrigger> throwItemTriggerCodec = RecordCodecBuilder.mapCodec(instance -> {
        return instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(o -> o.item)
        ).apply(instance, instance.stable(ThrowItemTrigger::new));
    });
    
    public static final MapCodec<ConventionalDimensionChangeTrigger> conventionalDimensionChangeCodec =
        MapCodec.unit(ConventionalDimensionChangeTrigger::new);
    
    static {
        codecRegistry = new MappedRegistry<>(
            ResourceKey.createRegistryKey(McHelper.newIdentifier("imm_ptl:custom_portal_gen_trigger")),
            Lifecycle.stable()
        );
        
        Registry.register(
            codecRegistry, McHelper.newIdentifier("imm_ptl:use_item"), useItemTriggerCodec
        );
        Registry.register(
            codecRegistry, McHelper.newIdentifier("imm_ptl:throw_item"), throwItemTriggerCodec
        );
        Registry.register(
            codecRegistry, McHelper.newIdentifier("imm_ptl:conventional_dimension_change"),
            ConventionalDimensionChangeTrigger.conventionalDimensionChangeCodec
        );
        
        triggerCodec = codecRegistry.byNameCodec().dispatchStable(
            PortalGenTrigger::getCodec,
            Function.identity()
        );
    }
    
    
}
