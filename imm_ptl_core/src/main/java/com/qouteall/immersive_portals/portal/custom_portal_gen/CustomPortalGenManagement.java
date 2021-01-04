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
import com.qouteall.immersive_portals.my_util.IntBox;
import com.qouteall.immersive_portals.my_util.UCoordinate;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.dynamic.RegistryOps;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.SimpleRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CustomPortalGenManagement {
    private static final Multimap<Item, CustomPortalGeneration> useItemGen = HashMultimap.create();
    private static final Multimap<Item, CustomPortalGeneration> throwItemGen = HashMultimap.create();
    
    private static final ArrayList<CustomPortalGeneration> convGen = new ArrayList<>();
    private static final Map<UUID, UCoordinate> posBeforeTravel = new HashMap<>();
    
    public static void onDatapackReload() {
        useItemGen.clear();
        throwItemGen.clear();
        convGen.clear();
        posBeforeTravel.clear();
        
        Helper.log("Loading custom portal gen");
        
        MinecraftServer server = McHelper.getServer();
        
        DynamicRegistryManager.Impl registryTracker =
            ((DynamicRegistryManager.Impl) server.getRegistryManager());
        
        RegistryOps<JsonElement> registryOps =
            RegistryOps.of(
                JsonOps.INSTANCE,
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
                CustomPortalGeneration.codec.codec()
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
        
        result.getEntries().forEach((entry) -> {
            CustomPortalGeneration gen = entry.getValue();
            gen.identifier = entry.getKey().getValue();
            
            if (!gen.initAndCheck()) {
                Helper.log("Custom Portal Gen Is Not Activated " + gen.toString());
                return;
            }
            
            Helper.log("Loaded Custom Portal Gen " + entry.getKey().getValue());
            
            load(gen);
            
            if (gen.reversible) {
                CustomPortalGeneration reverse = gen.getReverse();
                
                if (reverse != null) {
                    reverse.identifier = entry.getKey().getValue();
                    if (gen.initAndCheck()) {
                        load(reverse);
                    }
                }
                else {
                    McHelper.sendMessageToFirstLoggedPlayer(new LiteralText(
                        "Cannot create reverse generation of " + gen
                    ));
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
        else if (trigger instanceof PortalGenTrigger.ConventionalDimensionChangeTrigger) {
            convGen.add(gen);
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
                        context.getBlockPos().offset(context.getSide()),
                        context.getPlayer()
                    );
                    if (result) {
                        if (gen.trigger instanceof PortalGenTrigger.UseItemTrigger) {
                            PortalGenTrigger.UseItemTrigger trigger =
                                (PortalGenTrigger.UseItemTrigger) gen.trigger;
                            if (trigger.consume) {
                                context.getStack().decrement(1);
                            }
                        }
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
        if (entity.getThrower() == null) {
            return;
        }
        
        if (entity.cannotPickup()) {
            Item item = entity.getStack().getItem();
            if (throwItemGen.containsKey(item)) {
                ModMain.serverTaskList.addTask(() -> {
                    for (CustomPortalGeneration gen : throwItemGen.get(item)) {
                        boolean result = gen.perform(
                            ((ServerWorld) entity.world),
                            entity.getBlockPos(),
                            entity
                        );
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
    
    public static void onBeforeConventionalDimensionChange(
        ServerPlayerEntity player
    ) {
        posBeforeTravel.put(player.getUuid(), new UCoordinate(player));
    }
    
    public static void onAfterConventionalDimensionChange(
        ServerPlayerEntity player
    ) {
        UUID uuid = player.getUuid();
        if (posBeforeTravel.containsKey(uuid)) {
            UCoordinate startCoord = posBeforeTravel.get(uuid);
            posBeforeTravel.remove(uuid);
            
            ServerWorld startWorld = McHelper.getServerWorld(startCoord.dimension);
            
            BlockPos startPos = new BlockPos(startCoord.pos);
            
            for (CustomPortalGeneration gen : convGen) {
                IntBox box = new IntBox(startPos.add(-1, -1, -1), startPos.add(1, 1, 1));
                boolean succeeded = box.stream().anyMatch(pos -> gen.perform(startWorld, pos, player));
                
                if (succeeded) {
                    return;
                }
            }
        }
    }
}
