package qouteall.imm_ptl.peripheral.portal_generation;

import com.google.common.collect.Lists;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.dimension.AreaHelper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.portal.custom_portal_gen.CustomPortalGeneration;

import java.util.ArrayList;
import java.util.Optional;

public class IntrinsicPortalGeneration {
    public static final IntrinsicNetherPortalForm intrinsicNetherPortalForm =
        new IntrinsicNetherPortalForm();
    public static final DiligentNetherPortalForm diligentNetherPortalForm =
        new DiligentNetherPortalForm();
    public static final PortalHelperForm portalHelperForm =
        new PortalHelperForm();
    
    public static final CustomPortalGeneration intrinsicToNether = new CustomPortalGeneration(
        Lists.newArrayList(World.OVERWORLD), World.NETHER,
        8, 1,
        true,
        intrinsicNetherPortalForm,
        null, new ArrayList<>(), new ArrayList<>()
    );
    
    public static final CustomPortalGeneration intrinsicFromNether = intrinsicToNether.getReverse();
    
    public static final CustomPortalGeneration diligentToNether = new CustomPortalGeneration(
        Lists.newArrayList(World.OVERWORLD), World.NETHER,
        8, 1,
        true,
        diligentNetherPortalForm,
        null, new ArrayList<>(), new ArrayList<>()
    );
    
    public static final CustomPortalGeneration diligentFromNether = diligentToNether.getReverse();
    
    public static final CustomPortalGeneration portalHelper = new CustomPortalGeneration(
        Lists.newArrayList(CustomPortalGeneration.anyDimension),
        CustomPortalGeneration.theSameDimension,
        1, 1,
        false, portalHelperForm,
        null, new ArrayList<>(), new ArrayList<>()
    );
    
    public static void init() {
        intrinsicToNether.identifier = new Identifier("imm_ptl:intrinsic_nether_portal");
        intrinsicFromNether.identifier = new Identifier("imm_ptl:intrinsic_nether_portal");
        
        diligentFromNether.identifier = new Identifier("imm_ptl:intrinsic_diligent_nether_portal");
        diligentToNether.identifier = new Identifier("imm_ptl:intrinsic_diligent_nether_portal");
        
        portalHelper.identifier = new Identifier("imm_ptl:intrinsic_portal_helper");
    }
    
    public static boolean onFireLitOnObsidian(
        ServerWorld fromWorld,
        BlockPos firePos
    ) {
        RegistryKey<World> fromDimension = fromWorld.getRegistryKey();
        
        if (fromDimension == World.OVERWORLD) {
            CustomPortalGeneration gen =
                IPGlobal.netherPortalMode == IPGlobal.NetherPortalMode.normal ?
                    IntrinsicPortalGeneration.intrinsicToNether :
                    diligentToNether;
            return gen.perform(fromWorld, firePos, null);
        }
        else if (fromDimension == World.NETHER) {
            CustomPortalGeneration gen =
                IPGlobal.netherPortalMode == IPGlobal.NetherPortalMode.normal ?
                    IntrinsicPortalGeneration.intrinsicFromNether :
                    diligentFromNether;
            return gen.perform(fromWorld, firePos, null);
        }
        
        return false;
    }
    
    public static boolean activatePortalHelper(
        ServerWorld fromWorld,
        BlockPos firePos
    ) {
        return portalHelper.perform(fromWorld, firePos, null);
    }
    
    public static boolean onCrouchingPlayerIgnite(
        ServerWorld world,
        ServerPlayerEntity player,
        BlockPos firePos
    ) {
        if (IPGlobal.netherPortalMode == IPGlobal.NetherPortalMode.disabled) {
            return false;
        }
        
        if (!IPGlobal.lightVanillaNetherPortalWhenCrouching) {
            return false;
        }
        
        boolean dimensionCorrect =
            world.getRegistryKey() == World.OVERWORLD || world.getRegistryKey() == World.NETHER;
        
        if (!dimensionCorrect) {
            return false;
        }
        
        Optional<AreaHelper> newPortal = AreaHelper.getNewPortal(world, firePos, Direction.Axis.X);
        
        if (newPortal.isEmpty()) {
            return false;
        }
        
        AreaHelper areaHelper = newPortal.get();
        areaHelper.createPortal();
        return true;
    }
}
