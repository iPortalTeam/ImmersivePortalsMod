package com.qouteall.immersive_portals.portal.custom_portal_gen;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.resource.ServerResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.dynamic.RegistryOps;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryTracker;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.dimension.DimensionOptions;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class CustomPortalGenManagement {
    private static final Multimap<Item, CustomPortalGeneration> useItemGen = HashMultimap.create();
    private static final Multimap<Item, CustomPortalGeneration> throwItemGen = HashMultimap.create();
    
    public static void onServerStarted() {
        useItemGen.clear();
        throwItemGen.clear();
        
        MinecraftServer server = McHelper.getServer();
        
        RegistryTracker.Modifiable registryTracker = new RegistryTracker.Modifiable();
        
        RegistryOps<JsonElement> registryOps =
            RegistryOps.of(JsonOps.INSTANCE,
                server.serverResourceManager.getResourceManager(),
                registryTracker
            );
        
        SimpleRegistry<CustomPortalGeneration> emptyRegistry = new SimpleRegistry<>(
            CustomPortalGeneration.registryRegistryKey,
            Lifecycle.stable()
        );
        
        DataResult<SimpleRegistry<CustomPortalGeneration>> dataResult =
            registryOps.loadToRegistry(
                emptyRegistry,
                CustomPortalGeneration.registryRegistryKey,
                CustomPortalGeneration.codec
            );
        
        SimpleRegistry<CustomPortalGeneration> result = dataResult.getOrThrow(false, s -> {
            Helper.err("Cannot parse custom portal generation " + s);
        });
        
        result.stream().forEach(gen -> {
            Helper.log("Loaded Custom Portal Gen " + gen.toString());
            
            PortalGenTrigger trigger = gen.trigger;
            if (trigger instanceof PortalGenTrigger.UseItemTrigger) {
                useItemGen.put(((PortalGenTrigger.UseItemTrigger) trigger).item, gen);
            }
            else if (trigger instanceof PortalGenTrigger.ThrowItemTrigger) {
                throwItemGen.put(
                    ((PortalGenTrigger.ThrowItemTrigger) trigger).item,
                    gen
                );
            }
        });
    }
    
    public static void onItemUse(ItemUsageContext context, ActionResult actionResult) {
        for (CustomPortalGeneration gen : useItemGen.get(context.getStack().getItem())) {
            boolean result = gen.perform(((ServerWorld) context.getWorld()), context.getBlockPos());
            if (result) {
                return;
            }
        }
    }
    
    public static void onItemTick(ItemEntity entity) {
        if (entity.getThrower() != null) {
            for (CustomPortalGeneration gen : throwItemGen.get(entity.getStack().getItem())) {
                boolean result = gen.perform(((ServerWorld) entity.world), entity.getBlockPos());
                if (result) {
                    return;
                }
            }
        }
    }
}
