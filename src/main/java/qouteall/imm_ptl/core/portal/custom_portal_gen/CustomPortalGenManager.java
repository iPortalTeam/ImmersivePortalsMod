package qouteall.imm_ptl.core.portal.custom_portal_gen;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.IPPerServerInfo;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.mc_utils.ServerTaskList;
import qouteall.q_misc_util.my_util.WithDim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CustomPortalGenManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private final Multimap<Item, CustomPortalGeneration> useItemGen = HashMultimap.create();
    private final Multimap<Item, CustomPortalGeneration> throwItemGen = HashMultimap.create();
    
    private final ArrayList<CustomPortalGeneration> convGen = new ArrayList<>();
    private final Map<UUID, WithDim<Vec3>> playerPosBeforeTravel = new HashMap<>();
    
    public static void init() {
        DynamicRegistries.register(
            CustomPortalGeneration.REGISTRY_KEY,
            CustomPortalGeneration.CODEC
        );
        DynamicRegistries.register(
            CustomPortalGeneration.LEGACY_REGISTRY_KEY,
            CustomPortalGeneration.CODEC
        );
        
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(
            (server, resourceManager, success) -> onDataPackReloaded(server)
        );
        
        ServerLifecycleEvents.SERVER_STARTED.register(CustomPortalGenManager::onDataPackReloaded);
    }
    
    private static void onDataPackReloaded(MinecraftServer server) {
        if (!IPGlobal.enableDatapackPortalGen) {
            return;
        }
        
        LOGGER.info("Processing custom portal generation");
        
        CustomPortalGenManager manager = new CustomPortalGenManager();
        
        Registry<CustomPortalGeneration> registry = server.registryAccess()
            .registryOrThrow(CustomPortalGeneration.REGISTRY_KEY);
        Registry<CustomPortalGeneration> legacyRegistry = server.registryAccess()
            .registryOrThrow(CustomPortalGeneration.LEGACY_REGISTRY_KEY);
        
        for (var entry : registry.entrySet()) {
            manager.addEntry(server, entry.getKey(), entry.getValue());
        }
        
        for (var entry : legacyRegistry.entrySet()) {
            manager.addEntry(server, entry.getKey(), entry.getValue());
            ResourceLocation location = entry.getKey().location();
            String text = """
                [Immersive Portals]
                Custom portal generation config %s comes from legacy location
                /data/%s/custom_portal_generation/%s.json
                
                It's recommended to migrate it to the new location
                /data/%s/immersive_portals/custom_portal_generation/%s.json
                
                Future versions of the mod may no longer load generation configs from legacy locations.
                """
                .formatted(
                    location, location.getNamespace(), location.getPath(),
                    location.getNamespace(), location.getPath()
                );
            LOGGER.warn("{}", text);
//            McHelper.sendMessageToFirstLoggedPlayer(server, Component.literal(text));
        }
        
        IPPerServerInfo perServerInfo = IPPerServerInfo.of(server);
        perServerInfo.customPortalGenManager = manager;
    }
    
    private void addEntry(
        MinecraftServer server,
        ResourceKey<CustomPortalGeneration> key,
        CustomPortalGeneration gen
    ) {
        gen.identifier = key.location();
        
        CustomPortalGeneration.InitializationResult r1 = gen.initAndCheck(server);
        if (!(r1 instanceof CustomPortalGeneration.InitializationOk)) {
            LOGGER.info("Custom portal generation is not activated: {}\n{}", r1, gen.toString());
            return;
        }
        
        LOGGER.info("Loaded Custom Portal Generation {}", key.location());
        
        load(gen);
        
        if (gen.reversible) {
            CustomPortalGeneration reverse = gen.getReverse();
            
            if (reverse != null) {
                reverse.identifier = key.location();
                CustomPortalGeneration.InitializationResult r2 = reverse.initAndCheck(server);
                if (!(r2 instanceof CustomPortalGeneration.InitializationOk)) {
                    LOGGER.info(
                        "Reverse custom portal generation is not activated: {}\n{}",
                        r2, reverse.toString()
                    );
                    return;
                }
            }
            else {
                McHelper.sendMessageToFirstLoggedPlayer(
                    server,
                    Component.literal(
                        "Cannot create reverse generation of " + gen
                    )
                );
            }
        }
    }
    
    private void load(CustomPortalGeneration gen) {
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
    
    public void onItemUse(UseOnContext context, InteractionResult actionResult) {
        if (context.getLevel().isClientSide()) {
            return;
        }
        
        MinecraftServer server = context.getLevel().getServer();
        
        Item item = context.getItemInHand().getItem();
        if (useItemGen.containsKey(item)) {
            // perform it in the second tick
            ServerTaskList.of(server).addTask(() -> {
                for (CustomPortalGeneration gen : useItemGen.get(item)) {
                    boolean result = gen.perform(
                        ((ServerLevel) context.getLevel()),
                        context.getClickedPos().relative(context.getClickedFace()),
                        context.getPlayer()
                    );
                    if (result) {
                        if (gen.trigger instanceof PortalGenTrigger.UseItemTrigger trigger) {
                            if (trigger.shouldConsume(context)) {
                                context.getItemInHand().shrink(1);
                            }
                        }
                        break;
                    }
                }
                return true;
            });
        }
    }
    
    // only called when the item has a thrower.
    // avoid lagging when many non-thrown item entities exist (can come from things like tnt)
    // also does not check after pick-up delay, to reduce lagging
    public void onItemTick(ItemEntity entity) {
        Validate.isTrue(!entity.level().isClientSide());
        
        if (!entity.hasPickUpDelay()) {
            return;
        }
        
        Item item = entity.getItem().getItem();
        Collection<CustomPortalGeneration> gens = throwItemGen.get(item);
        if (gens.isEmpty()) {
            return;
        }
        
        ServerTaskList.of(entity.getServer()).addTask(() -> {
            for (CustomPortalGeneration gen : gens) {
                boolean result = gen.perform(
                    ((ServerLevel) entity.level()),
                    entity.blockPosition(),
                    entity
                );
                if (result) {
                    entity.getItem().shrink(1);
                    break;
                }
            }
            return true;
        });
    }
    
    public void onBeforeConventionalDimensionChange(
        ServerPlayer player
    ) {
        playerPosBeforeTravel.put(
            player.getUUID(), new WithDim<>(player.level().dimension(), player.position())
        );
    }
    
    public void onAfterConventionalDimensionChange(
        ServerPlayer player
    ) {
        UUID uuid = player.getUUID();
        if (playerPosBeforeTravel.containsKey(uuid)) {
            WithDim<Vec3> startCoord = playerPosBeforeTravel.get(uuid);
            
            ServerLevel startWorld = player.server.getLevel(startCoord.dimension());
            if (startWorld == null) {
                LOGGER.error("Cannot find world {}", startCoord.dimension());
                return;
            }
            
            BlockPos startPos = BlockPos.containing(startCoord.value());
            
            for (CustomPortalGeneration gen : convGen) {
                boolean succeeded = gen.perform(startWorld, startPos, player);
                
                if (succeeded) {
                    playerPosBeforeTravel.remove(uuid);
                    return;
                }
            }
        }
        playerPosBeforeTravel.remove(uuid);
    }
}
