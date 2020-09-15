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

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class CustomPortalGeneration {
    public static final RegistryKey<World> theSameDimension = RegistryKey.of(
        Registry.DIMENSION,
        new Identifier("imm_ptl:the_same_dimension")
    );
    
    public static final RegistryKey<World> anyDimension = RegistryKey.of(
        Registry.DIMENSION,
        new Identifier("imm_ptl:any_dimension")
    );
    
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
            Codec.BOOL.optionalFieldOf("reversible", true).forGetter(o -> o.reversible),
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
    
    public static final MapCodec<CustomPortalGeneration> codec = schemaRegistry.dispatchMap(
        "schema_version", e -> codecV1, Function.identity()
    );
    
    
    public final List<RegistryKey<World>> fromDimensions;
    public final RegistryKey<World> toDimension;
    public final int spaceRatioFrom;
    public final int spaceRatioTo;
    public final boolean reversible;
    public final PortalGenForm form;
    public final PortalGenTrigger trigger;
    public final List<String> postInvokeCommands;
    
    public Identifier identifier = null;
    
    public CustomPortalGeneration(
        List<RegistryKey<World>> fromDimensions, RegistryKey<World> toDimension,
        int spaceRatioFrom, int spaceRatioTo, boolean reversible,
        PortalGenForm form, PortalGenTrigger trigger,
        List<String> postInvokeCommands
    ) {
        this.fromDimensions = fromDimensions;
        this.toDimension = toDimension;
        this.spaceRatioFrom = spaceRatioFrom;
        this.spaceRatioTo = spaceRatioTo;
        this.reversible = reversible;
        this.form = form;
        this.trigger = trigger;
        this.postInvokeCommands = postInvokeCommands;
    }
    
    @Nullable
    public CustomPortalGeneration getReverse() {
        if (toDimension == theSameDimension) {
            return new CustomPortalGeneration(
                fromDimensions,
                theSameDimension,
                spaceRatioTo,
                spaceRatioFrom,
                false,
                form.getReverse(),
                trigger,
                postInvokeCommands
            );
        }
        
        if (!fromDimensions.isEmpty()) {
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
        // if from dimension is not present, nothing happens
        
        RegistryKey<World> toDimension = this.toDimension;
        if (toDimension != theSameDimension) {
            if (McHelper.getServer().getWorld(toDimension) == null) {
                return false;
            }
        }
        
        if (!form.initAndCheck()) {
            return false;
        }
        
        if (fromDimensions.isEmpty()) {
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
            if (fromDimensions.get(0) != anyDimension) {
                return false;
            }
        }
        
        if (!world.isChunkLoaded(startPos)) {
            Helper.log("Skip custom portal generation because chunk not loaded");
            return false;
        }
        
        RegistryKey<World> destDimension = this.toDimension;
        
        if (destDimension == theSameDimension) {
            destDimension = world.getRegistryKey();
        }
        
        ServerWorld toWorld = McHelper.getServer().getWorld(destDimension);
        
        if (toWorld == null) {
            Helper.err("Missing dimension " + destDimension.getValue());
            return false;
        }
        
        world.getProfiler().push("custom_portal_gen_perform");
        boolean result = form.perform(this, world, startPos, toWorld);
        world.getProfiler().pop();
        return result;
    }
    
    public void onPortalGenerated(Portal portal) {
        if (identifier != null) {
            portal.portalTag = identifier.toString();
        }
        
        if (postInvokeCommands.isEmpty()) {
            return;
        }
        
        ServerCommandSource commandSource = portal.getCommandSource().withLevel(4).withSilent();
        CommandManager commandManager = McHelper.getServer().getCommandManager();
        
        for (String command : postInvokeCommands) {
            commandManager.execute(commandSource, command);
        }
    }
}
