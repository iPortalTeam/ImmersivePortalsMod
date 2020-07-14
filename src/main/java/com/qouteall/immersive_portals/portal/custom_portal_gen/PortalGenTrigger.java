package com.qouteall.immersive_portals.portal.custom_portal_gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;

import java.util.function.Function;

public abstract class PortalGenTrigger {
    public static final Codec<PortalGenTrigger> triggerCodec;
    
    public static final Registry<Codec<? extends PortalGenTrigger>> codecRegistry;
    
    public abstract Codec<? extends PortalGenTrigger> getCodec();
    
    public static class UseItemTrigger extends PortalGenTrigger {
        public Item item;
        
        public UseItemTrigger(Item item) {
            this.item = item;
        }
        
        @Override
        public Codec<? extends PortalGenTrigger> getCodec() {
            return useItemTriggerCodec;
        }
    }
    
    public static class ThrowItemTrigger extends PortalGenTrigger {
        public Item item;
        
        public ThrowItemTrigger(Item item) {
            this.item = item;
        }
    
        @Override
        public Codec<? extends PortalGenTrigger> getCodec() {
            return throwItemTriggerCodec;
        }
    }
    
    public static final Codec<UseItemTrigger> useItemTriggerCodec = RecordCodecBuilder.create(instance -> {
        return instance.group(
            Registry.ITEM.fieldOf("item").forGetter(o -> o.item)
        ).apply(instance, instance.stable(UseItemTrigger::new));
    });
    
    
    public static final Codec<ThrowItemTrigger> throwItemTriggerCodec = RecordCodecBuilder.create(instance -> {
        return instance.group(
            Registry.ITEM.fieldOf("item").forGetter(o -> o.item)
        ).apply(instance, instance.stable(ThrowItemTrigger::new));
    });
    
    static {
        codecRegistry = new SimpleRegistry<>(
            RegistryKey.ofRegistry(new Identifier("imm_ptl:custom_portal_gen_trigger")),
            Lifecycle.stable()
        );
        
        Registry.register(
            codecRegistry, new Identifier("imm_ptl:use_item"), useItemTriggerCodec
        );
        Registry.register(
            codecRegistry, new Identifier("imm_ptl:throw_item"), throwItemTriggerCodec
        );
    
        triggerCodec = codecRegistry.dispatchStable(
            PortalGenTrigger::getCodec,
            Function.identity()
        );
    }
    
}
