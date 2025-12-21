package qouteall.imm_ptl.core.portal.custom_portal_gen.form;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.portal.custom_portal_gen.CustomPortalGeneration;

import java.util.function.Function;

public abstract class PortalGenForm {
    public static final Registry<MapCodec<? extends PortalGenForm>> CODEC_REGISTRY = Util.make(() -> {
        MappedRegistry<MapCodec<? extends PortalGenForm>> registry = new MappedRegistry<>(
            ResourceKey.createRegistryKey(McHelper.newIdentifier("imm_ptl:custom_portal_gen_form")),
            Lifecycle.stable()
        );
        
        Registry.register(
            registry, McHelper.newIdentifier("imm_ptl:classical"), ClassicalForm.CODEC
        );
        Registry.register(
            registry, McHelper.newIdentifier("imm_ptl:heterogeneous"), HeterogeneousForm.CODEC
        );
        Registry.register(
            registry, McHelper.newIdentifier("imm_ptl:flipping_floor_square"), FlippingFloorSquareForm.CODEC
        );
        Registry.register(
            registry, McHelper.newIdentifier("imm_ptl:scaling_square"), ScalingSquareForm.CODEC
        );
        Registry.register(
            registry, McHelper.newIdentifier("imm_ptl:flipping_floor_square_new"), FlippingFloorSquareNewForm.CODEC
        );
        Registry.register(
            registry, McHelper.newIdentifier("imm_ptl:try_hard_to_match"), DiligentForm.CODEC
        );
        Registry.register(
            registry, McHelper.newIdentifier("imm_ptl:convert_conventional_portal"), ConvertConventionalPortalForm.CODEC
        );
        Registry.register(
            registry, McHelper.newIdentifier("imm_ptl:one_way"), OneWayForm.CODEC
        );
        
        return registry;
    });
    
    public static final Codec<PortalGenForm> GENERAL_CODEC =
        CODEC_REGISTRY.byNameCodec().dispatchStable(
            PortalGenForm::getCodec, Function.identity()
        );
    
    public abstract MapCodec<? extends PortalGenForm> getCodec();
    
    public abstract PortalGenForm getReverse();
    
    // Return true for succeeded
    public abstract boolean perform(
        CustomPortalGeneration cpg,
        ServerLevel fromWorld, BlockPos startingPos,
        ServerLevel toWorld,
        @Nullable Entity triggeringEntity
    );
}
