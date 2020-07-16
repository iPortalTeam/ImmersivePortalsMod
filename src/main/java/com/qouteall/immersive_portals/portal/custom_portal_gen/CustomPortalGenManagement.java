package com.qouteall.immersive_portals.portal.custom_portal_gen;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import net.minecraft.block.Block;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.RegistryTagManager;
import net.minecraft.tag.Tag;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.RegistryOps;
import net.minecraft.util.registry.RegistryTracker;
import net.minecraft.util.registry.SimpleRegistry;

import javax.annotation.Nullable;

public class CustomPortalGenManagement {
    private static final Multimap<Item, CustomPortalGeneration> useItemGen = HashMultimap.create();
    private static final Multimap<Item, CustomPortalGeneration> throwItemGen = HashMultimap.create();
    
    public static void onDatapackReload() {
        useItemGen.clear();
        throwItemGen.clear();
        
        Helper.log("Loading custom portal gen");
        
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
        
        SimpleRegistry<CustomPortalGeneration> result = dataResult.get().left().orElse(null);
        
        if (result == null) {
            DataResult.PartialResult<SimpleRegistry<CustomPortalGeneration>> r =
                dataResult.get().right().get();
            McHelper.sendMessageToFirstLoggedPlayer(new LiteralText(
                "Error when parsing custom portal generation\n" +
                    r.message()
            ));
            return;
        }
        
        result.stream().forEach(gen -> {
            if (!gen.initAndCheck()) {
                Helper.log("Custom Portal Gen Is Not Activated " + gen.toString());
                return;
            }
            
            Helper.log("Loaded Custom Portal Gen " + gen.toString());
            
            load(gen);
            
            if (gen.twoWay) {
                CustomPortalGeneration reverse = gen.getReverse();
                if (gen.initAndCheck()) {
                    load(reverse);
                }
            }
        });
    }
    
    private static void load(CustomPortalGeneration gen) {
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
    }
    
    public static void onItemUse(ItemUsageContext context, ActionResult actionResult) {
        if (context.getWorld().isClient()) {
            return;
        }
        
        Item item = context.getStack().getItem();
        if (useItemGen.containsKey(item)) {
            ModMain.serverTaskList.addTask(() -> {
                for (CustomPortalGeneration gen : useItemGen.get(item)) {
                    boolean result = gen.perform(
                        ((ServerWorld) context.getWorld()),
                        context.getBlockPos().offset(context.getSide())
                    );
                    if (result) {
                        break;
                    }
                }
                return true;
            });
        }
    }
    
    public static void onItemTick(ItemEntity entity) {
        if (entity.world.isClient()) {
            return;
        }
        if (entity.getThrower() != null) {
            Item item = entity.getStack().getItem();
            if (throwItemGen.containsKey(item)) {
                ModMain.serverTaskList.addTask(() -> {
                    for (CustomPortalGeneration gen : throwItemGen.get(item)) {
                        boolean result = gen.perform(((ServerWorld) entity.world), entity.getBlockPos());
                        if (result) {
                            entity.getStack().decrement(1);
                            break;
                        }
                    }
                    return true;
                });
            }
        }
    }
}
