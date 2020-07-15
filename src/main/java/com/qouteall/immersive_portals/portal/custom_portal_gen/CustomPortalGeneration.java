package com.qouteall.immersive_portals.portal.custom_portal_gen;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.ListCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.custom_portal_gen.form.PortalGenForm;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class CustomPortalGeneration {
    public static final Codec<List<RegistryKey<World>>> dimensionListCodec =
        new ListCodec<>(World.CODEC);
    public static final Codec<List<String>> stringListCodec =
        new ListCodec<>(Codec.STRING);
    
    public static RegistryKey<Registry<Codec<CustomPortalGeneration>>> schemaRegistryKey = RegistryKey.ofRegistry(
        new Identifier("imm_ptl:custom_portal_gen_schema")
    );
    
    public static RegistryKey<Registry<CustomPortalGeneration>> registryRegistryKey =
        RegistryKey.ofRegistry(new Identifier("imm_ptl:custom_portal_generation"));
    
    public static final Codec<CustomPortalGeneration> codecV1 = RecordCodecBuilder.create(instance -> {
        return instance.group(
            dimensionListCodec.fieldOf("from").forGetter(o -> o.fromDimensions),
            World.CODEC.fieldOf("to").forGetter(o -> o.toDimension),
            Codec.INT.optionalFieldOf("space_ratio_from", 1).forGetter(o -> o.spaceRatioFrom),
            Codec.INT.optionalFieldOf("space_ratio_to", 1).forGetter(o -> o.spaceRatioTo),
            Codec.BOOL.fieldOf("two_way").forGetter(o -> o.twoWay),
            PortalGenForm.codec.fieldOf("form").forGetter(o -> o.form),
            PortalGenTrigger.triggerCodec.fieldOf("trigger").forGetter(o -> o.trigger),
            stringListCodec.optionalFieldOf("post_invoke_commands", Collections.emptyList())
                .forGetter(o -> o.postInvokeCommands)
        ).apply(instance, instance.stable(CustomPortalGeneration::new));
    });
    
    public static SimpleRegistry<Codec<CustomPortalGeneration>> schemaRegistry = Util.make(() -> {
        SimpleRegistry<Codec<CustomPortalGeneration>> registry = new SimpleRegistry<>(
            schemaRegistryKey, Lifecycle.stable()
        );
        Registry.register(
            registry, new Identifier("imm_ptl:v1"), codecV1
        );
        return registry;
    });
    
    public static final MapCodec<CustomPortalGeneration> codec = schemaRegistry.dispatchStableMap(
        "schema_version", e -> codecV1, Function.identity()
    );
    
    
    public final List<RegistryKey<World>> fromDimensions;
    public final RegistryKey<World> toDimension;
    public final int spaceRatioFrom;
    public final int spaceRatioTo;
    public final boolean twoWay;
    public final PortalGenForm form;
    public final PortalGenTrigger trigger;
    public final List<String> postInvokeCommands;
    
    public CustomPortalGeneration(
        List<RegistryKey<World>> fromDimensions, RegistryKey<World> toDimension,
        int spaceRatioFrom, int spaceRatioTo, boolean twoWay,
        PortalGenForm form, PortalGenTrigger trigger,
        List<String> postInvokeCommands
    ) {
        this.fromDimensions = fromDimensions;
        this.toDimension = toDimension;
        this.spaceRatioFrom = spaceRatioFrom;
        this.spaceRatioTo = spaceRatioTo;
        this.twoWay = twoWay;
        this.form = form;
        this.trigger = trigger;
        this.postInvokeCommands = postInvokeCommands;
    }
    
    public CustomPortalGeneration getReverse() {
        if (fromDimensions.size() == 1) {
            return new CustomPortalGeneration(
                Lists.newArrayList(toDimension),
                fromDimensions.get(0),
                spaceRatioTo,
                spaceRatioFrom,
                false,
                form.getReverse(),
                trigger,
                postInvokeCommands
            );
        }
        
        Helper.err("Cannot get reverse custom portal gen");
        return null;
    }
    
    public BlockPos mapPosition(BlockPos from) {
        return Helper.divide(Helper.scale(from, spaceRatioTo), spaceRatioFrom);
    }
    
    public boolean initAndCheck() {
        // if the dimension is missing, do not load
        fromDimensions.removeIf(dim -> McHelper.getServer().getWorld(dim) == null);
        if (fromDimensions.isEmpty()) {
            return false;
        }
        
        if (McHelper.getServer().getWorld(toDimension) == null) {
            return false;
        }
        
        if (!form.initAndCheck()) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public String toString() {
        return McHelper.serializeToJson(
            this,
            codec.codec()
        );
    }
    
    public boolean perform(ServerWorld world, BlockPos startPos) {
        if (!fromDimensions.contains(world.getRegistryKey())) {
            return false;
        }
        
        if (!world.isChunkLoaded(startPos)) {
            return false;
        }
        
        ServerWorld toWorld = McHelper.getServer().getWorld(toDimension);
        
        if (toWorld == null) {
            Helper.err("Missing dimension " + toDimension.getValue());
            return false;
        }
        
        world.getProfiler().push("custom_portal_gen_perform");
        boolean result = form.perform(this, world, startPos, toWorld);
        world.getProfiler().pop();
        return result;
    }
    
    public void onPortalGenerated(Portal portal) {
        if (postInvokeCommands.isEmpty()) {
            return;
        }
    
        ServerCommandSource commandSource = portal.getCommandSource();
        CommandManager commandManager = McHelper.getServer().getCommandManager();
    
        for (String command : postInvokeCommands) {
            commandManager.execute(commandSource, command);
        }
    }
}
