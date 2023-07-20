package qouteall.imm_ptl.core.portal.custom_portal_gen.form;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import qouteall.imm_ptl.core.portal.custom_portal_gen.CustomPortalGeneration;

import org.jetbrains.annotations.Nullable;
import java.util.function.Function;

public abstract class PortalGenForm {
    public static final Registry<Codec<? extends PortalGenForm>> codecRegistry = Util.make(() -> {
        MappedRegistry<Codec<? extends PortalGenForm>> registry = new MappedRegistry<>(
            ResourceKey.createRegistryKey(new ResourceLocation("imm_ptl:custom_portal_gen_form")),
            Lifecycle.stable()
        );
        
        Registry.register(
            registry, new ResourceLocation("imm_ptl:classical"), ClassicalForm.codec
        );
        Registry.register(
            registry, new ResourceLocation("imm_ptl:heterogeneous"), HeterogeneousForm.codec
        );
        Registry.register(
            registry, new ResourceLocation("imm_ptl:flipping_floor_square"), FlippingFloorSquareForm.codec
        );
        Registry.register(
            registry, new ResourceLocation("imm_ptl:scaling_square"), ScalingSquareForm.codec
        );
        Registry.register(
            registry, new ResourceLocation("imm_ptl:flipping_floor_square_new"), FlippingFloorSquareNewForm.codec
        );
        Registry.register(
            registry, new ResourceLocation("imm_ptl:try_hard_to_match"), DiligentForm.codec
        );
        Registry.register(
            registry, new ResourceLocation("imm_ptl:convert_conventional_portal"), ConvertConventionalPortalForm.codec
        );
        Registry.register(
            registry, new ResourceLocation("imm_ptl:one_way"), OneWayForm.codec
        );
        
        return registry;
    });
    
    public static final Codec<PortalGenForm> codec =
        codecRegistry.byNameCodec().dispatchStable(
            PortalGenForm::getCodec, Function.identity()
        );
    
    public abstract Codec<? extends PortalGenForm> getCodec();
    
    public abstract PortalGenForm getReverse();
    
    // Return true for succeeded
    public abstract boolean perform(
        CustomPortalGeneration cpg,
        ServerLevel fromWorld, BlockPos startingPos,
        ServerLevel toWorld,
        @Nullable Entity triggeringEntity
    );
    
    public boolean initAndCheck() {
        return true;
    }
}
