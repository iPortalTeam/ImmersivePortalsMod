package qouteall.imm_ptl.core.portal.custom_portal_gen;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.ListCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.custom_portal_gen.form.PortalGenForm;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;

import org.jetbrains.annotations.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class CustomPortalGeneration {
    public static final ResourceKey<Level> theSameDimension = ResourceKey.create(
        Registries.DIMENSION,
        new ResourceLocation("imm_ptl:the_same_dimension")
    );
    
    public static final ResourceKey<Level> anyDimension = ResourceKey.create(
        Registries.DIMENSION,
        new ResourceLocation("imm_ptl:any_dimension")
    );
    
    public static final Codec<List<ResourceKey<Level>>> dimensionListCodec =
        new ListCodec<>(Level.RESOURCE_KEY_CODEC);
    public static final Codec<List<String>> stringListCodec =
        new ListCodec<>(Codec.STRING);
    public static final Codec<List<List<String>>> stringListListCodec =
        new ListCodec<>(stringListCodec);
    
    public static ResourceKey<Registry<Codec<CustomPortalGeneration>>> schemaRegistryKey = ResourceKey.createRegistryKey(
        new ResourceLocation("imm_ptl:custom_portal_gen_schema")
    );
    
    public static ResourceKey<Registry<CustomPortalGeneration>> registryRegistryKey =
        ResourceKey.createRegistryKey(new ResourceLocation("imm_ptl:custom_portal_generation"));
    
    public static final Codec<CustomPortalGeneration> codecV1 =
        RecordCodecBuilder.create(instance -> {
            return instance.group(
                dimensionListCodec.fieldOf("from").forGetter(o -> o.fromDimensions),
                Level.RESOURCE_KEY_CODEC.fieldOf("to").forGetter(o -> o.toDimension),
                Codec.INT.optionalFieldOf("space_ratio_from", 1).forGetter(o -> o.spaceRatioFrom),
                Codec.INT.optionalFieldOf("space_ratio_to", 1).forGetter(o -> o.spaceRatioTo),
                Codec.BOOL.optionalFieldOf("reversible", true).forGetter(o -> o.reversible),
                PortalGenForm.codec.fieldOf("form").forGetter(o -> o.form),
                PortalGenTrigger.triggerCodec.fieldOf("trigger").forGetter(o -> o.trigger),
                stringListCodec.optionalFieldOf("post_invoke_commands", Collections.emptyList())
                    .forGetter(o -> o.postInvokeCommands),
                stringListListCodec.optionalFieldOf("commands_on_generated", Collections.emptyList())
                    .forGetter(o -> o.commandsOnGenerated)
            ).apply(instance, instance.stable(CustomPortalGeneration::new));
        });
    
    public static MappedRegistry<Codec<CustomPortalGeneration>> schemaRegistry =
        Util.make(() -> {
            MappedRegistry<Codec<CustomPortalGeneration>> registry = new MappedRegistry<>(
                schemaRegistryKey, Lifecycle.stable()
            );
            Registry.register(
                registry, new ResourceLocation("imm_ptl:v1"), codecV1
            );
            return registry;
        });
    
    public static final MapCodec<CustomPortalGeneration> codec =
        schemaRegistry.byNameCodec().dispatchMap(
            "schema_version", e -> codecV1, Function.identity()
        );
    
    public final List<ResourceKey<Level>> fromDimensions;
    public final ResourceKey<Level> toDimension;
    public final int spaceRatioFrom;
    public final int spaceRatioTo;
    public final boolean reversible;
    public final PortalGenForm form;
    public final PortalGenTrigger trigger;
    public final List<String> postInvokeCommands;
    public final List<List<String>> commandsOnGenerated;
    
    public ResourceLocation identifier = null;
    
    public CustomPortalGeneration(
        List<ResourceKey<Level>> fromDimensions, ResourceKey<Level> toDimension,
        int spaceRatioFrom, int spaceRatioTo, boolean reversible,
        PortalGenForm form, PortalGenTrigger trigger,
        List<String> postInvokeCommands,
        List<List<String>> commandsOnGenerated
    ) {
        this.fromDimensions = fromDimensions;
        this.toDimension = toDimension;
        this.spaceRatioFrom = spaceRatioFrom;
        this.spaceRatioTo = spaceRatioTo;
        this.reversible = reversible;
        this.form = form;
        this.trigger = trigger;
        this.postInvokeCommands = postInvokeCommands;
        this.commandsOnGenerated = commandsOnGenerated;
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
                postInvokeCommands,
                commandsOnGenerated
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
                postInvokeCommands,
                commandsOnGenerated
            );
        }
        
        Helper.err("Cannot get reverse custom portal gen");
        return null;
    }
    
    public BlockPos mapPosition(
        BlockPos from,
        ServerLevel fromWorld, ServerLevel toWorld
    ) {
        BlockPos newPosition = Helper.divide(Helper.scale(from, spaceRatioTo), spaceRatioFrom);
        
        boolean withinBounds = toWorld.getWorldBorder().isWithinBounds(newPosition);
        if (!withinBounds) {
            Helper.log("Tries to spawn a portal outside of world border");
            BlockPos clamped = toWorld.getWorldBorder().clampToBounds(
                newPosition.getX(), newPosition.getY(), newPosition.getZ()
            );
            newPosition = new BlockPos(
                (int) (clamped.getX() * 0.9), clamped.getY(), (int) (clamped.getZ() * 0.9)
            );
        }
        
        return newPosition;
    }
    
    public boolean initAndCheck() {
        // if from dimension is not present, nothing happens
        
        ResourceKey<Level> toDimension = this.toDimension;
        if (toDimension != theSameDimension) {
            if (MiscHelper.getServer().getLevel(toDimension) == null) {
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
    
    public boolean perform(
        ServerLevel world,
        BlockPos startPos,
        @Nullable Entity triggeringEntity
    ) {
        if (!fromDimensions.contains(world.dimension())) {
            if (fromDimensions.get(0) != anyDimension) {
                return false;
            }
        }
        
        if (!world.hasChunkAt(startPos)) {
            Helper.log("Skip custom portal generation because chunk not loaded");
            return false;
        }
        
        ResourceKey<Level> destDimension = this.toDimension;
        
        if (destDimension == theSameDimension) {
            destDimension = world.dimension();
        }
        
        ServerLevel toWorld = MiscHelper.getServer().getLevel(destDimension);
        
        if (toWorld == null) {
            Helper.err("Missing dimension " + destDimension.location());
            return false;
        }
        
        world.getProfiler().push("custom_portal_gen_perform");
        boolean result = form.perform(this, world, startPos, toWorld, triggeringEntity);
        world.getProfiler().pop();
        return result;
    }
    
    public void onPortalsGenerated(Portal[] portals) {
        for (int i = 0; i < portals.length; i++) {
            Portal portal = portals[i];
            if (identifier != null) {
                portal.portalTag = identifier.toString();
            }
            
            if (!postInvokeCommands.isEmpty()) {
                McHelper.invokeCommandAs(portal, postInvokeCommands);
            }
            
            if (i < commandsOnGenerated.size()) {
                List<String> commandsForThisPortal = commandsOnGenerated.get(i);
                McHelper.invokeCommandAs(portal, commandsForThisPortal);
            }
        }
        
        
    }
}
