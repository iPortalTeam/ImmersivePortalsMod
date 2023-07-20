package qouteall.imm_ptl.core.portal.custom_portal_gen.form;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import qouteall.imm_ptl.core.portal.custom_portal_gen.CustomPortalGeneration;
import qouteall.imm_ptl.core.portal.custom_portal_gen.PortalGenInfo;
import qouteall.imm_ptl.core.portal.custom_portal_gen.SimpleBlockPredicate;
import qouteall.imm_ptl.core.portal.nether_portal.BlockPortalShape;
import qouteall.imm_ptl.core.portal.nether_portal.BlockTraverse;
import qouteall.imm_ptl.core.portal.nether_portal.BreakablePortalEntity;
import qouteall.imm_ptl.core.portal.nether_portal.GeneralBreakablePortal;
import qouteall.imm_ptl.core.portal.nether_portal.NetherPortalGeneration;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.IntBox;

import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ConvertConventionalPortalForm extends PortalGenForm {
    
    public static final Codec<ConvertConventionalPortalForm> codec = RecordCodecBuilder.create(instance -> {
        return instance.group(
            SimpleBlockPredicate.codec.fieldOf("portal_block").forGetter(o -> o.portalBlock)
        ).apply(instance, instance.stable(ConvertConventionalPortalForm::new));
    });
    
    public final SimpleBlockPredicate portalBlock;
    
    public ConvertConventionalPortalForm(SimpleBlockPredicate portalBlock) {
        this.portalBlock = portalBlock;
    }
    
    @Override
    public Codec<? extends PortalGenForm> getCodec() {
        return codec;
    }
    
    @Override
    public PortalGenForm getReverse() {
        return this;
    }
    
    @Override
    public boolean perform(
        CustomPortalGeneration cpg, ServerLevel fromWorld,
        BlockPos startingPos, ServerLevel toWorld,
        @Nullable Entity triggeringEntity
    ) {
        if (triggeringEntity == null) {
            Helper.err("Null triggering entity for portal conversion");
            return false;
        }
        
        if (!(triggeringEntity instanceof ServerPlayer)) {
            Helper.err("Non player entity triggers portal conversion");
            return false;
        }
        
        ServerPlayer player = (ServerPlayer) triggeringEntity;
        
        
        if (player.level() != toWorld) {
            Helper.err("The player is not in the correct world " +
                player.level().dimension().location());
            return false;
        }
        
        BlockPos playerCurrentPos = player.blockPosition().immutable();
        
        BlockPos startFramePos = findBlockAround(
            fromWorld, startingPos, portalBlock
        );
        
        if (startFramePos == null) {
            return false;
        }
        
        BlockPos toFramePos = findBlockAround(
            toWorld, playerCurrentPos, portalBlock
        );
        
        if (toFramePos == null) {
            return false;
        }
        
        Helper.log(String.format(
            "Trying to convert conventional portal %s -> %s by %s (%d %d %d)",
            fromWorld.dimension().location(),
            toWorld.dimension().location(),
            player.getName().getContents(),
            (int) player.getX(), (int) player.getY(), (int) player.getZ()
        ));
        
        BlockPortalShape fromShape = NetherPortalGeneration.findFrameShape(
            fromWorld, startFramePos, portalBlock, s -> !s.isAir()
        );
        
        if (fromShape == null) {
            Helper.err("Cannot find from side shape");
            return false;
        }
        
        BlockPortalShape toShape = NetherPortalGeneration.findFrameShape(
            toWorld, toFramePos, portalBlock, s -> !s.isAir()
        );
        
        if (toShape == null) {
            Helper.err("Cannot fine to side shape");
            return false;
        }
        
        Helper.log(fromShape.innerAreaBox + " " + toShape.innerAreaBox);
        
        PortalGenInfo portalGenInfo = tryToMatch(
            fromWorld.dimension(), toWorld.dimension(),
            fromShape, toShape
        );
        
        if (portalGenInfo == null) {
            Helper.err("Shapes are incompatible");
            player.displayClientMessage(
                Component.translatable(
                    "imm_ptl.incompatible_shape"
                ), false
            );
            
            return false;
        }
        
        portalGenInfo.generatePlaceholderBlocks();
        
        if (fromShape.axis == Direction.Axis.Y &&
            toShape.axis == Direction.Axis.Y &&
            portalGenInfo.scale == 1.0 &&
            portalGenInfo.rotation == null
        ) {
            //flipping square portal
            GeneralBreakablePortal[] portals = FlippingFloorSquareForm.createPortals(
                fromWorld, toWorld,
                portalGenInfo.fromShape, portalGenInfo.toShape
            );
    
            cpg.onPortalsGenerated(portals);
            
            
            Helper.log("Created flipping floor portal");
        }
        else {
            BreakablePortalEntity[] portals =
                portalGenInfo.generateBiWayBiFacedPortal(GeneralBreakablePortal.entityType);
    
            cpg.onPortalsGenerated(portals);
            
            Helper.log("Created normal bi-way bi-faced portal");
            
        }
        
        return true;
    }
    
    @Deprecated
    @Nullable
    public static IntBox findBlockBoxArea(
        Level world, BlockPos pos, Predicate<BlockState> predicate
    ) {
        BlockPos startingPos = findBlockAround(world, pos, predicate);
        
        if (startingPos == null) {
            return null;
        }
        
        IntBox result = Helper.expandBoxArea(
            startingPos,
            p -> predicate.test(world.getBlockState(p))
        );
        
        if (result.getSize().equals(new BlockPos(1, 1, 1))) {
            return null;
        }
        
        return result;
    }
    
    @Nullable
    public static BlockPos findBlockAround(
        Level world, BlockPos pos, Predicate<BlockState> predicate
    ) {
        BlockState blockState = world.getBlockState(pos);
        if (predicate.test(blockState)) {
            return pos;
        }
        
        return BlockTraverse.searchInBox(
            new IntBox(pos.offset(-2, -2, -2), pos.offset(2, 2, 2)),
            p -> {
                if (predicate.test(world.getBlockState(p))) {
                    return p;
                }
                return null;
            }
        );
    }
    
    @Deprecated
    @Nullable
    public static BlockPortalShape convertToPortalShape(IntBox box) {
        BlockPos size = box.getSize();
        Direction.Axis axis = null;
        if (size.getX() == 1) {
            axis = Direction.Axis.X;
        }
        else if (size.getY() == 1) {
            axis = Direction.Axis.Y;
        }
        else if (size.getZ() == 1) {
            axis = Direction.Axis.Z;
        }
        else {
            Helper.err("The box is not flat " + box);
            return null;
        }
        
        return new BlockPortalShape(
            box.stream().collect(Collectors.toSet()),
            axis
        );
    }
    
    @Nullable
    public static PortalGenInfo tryToMatch(
        ResourceKey<Level> fromDim, ResourceKey<Level> toDim,
        BlockPortalShape a, BlockPortalShape b
    ) {
        List<DiligentMatcher.TransformedShape> matchableShapeVariants =
            DiligentMatcher.getMatchableShapeVariants(
                a, BlockPortalShape.defaultLengthLimit
            );
        
        for (DiligentMatcher.TransformedShape variant : matchableShapeVariants) {
            BlockPortalShape variantMoved = variant.transformedShape.getShapeWithMovedAnchor(b.anchor);
            if (variantMoved.equals(b)) {
                return new PortalGenInfo(
                    fromDim, toDim,
                    a, b,
                    variant.rotation.toQuaternion(),
                    variant.scale
                );
            }
        }
        
        return null;
    }
}
