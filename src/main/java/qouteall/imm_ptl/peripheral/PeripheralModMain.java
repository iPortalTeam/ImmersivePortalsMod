package qouteall.imm_ptl.peripheral;

import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.portal.EndPortalEntity;
import qouteall.imm_ptl.peripheral.alternate_dimension.AlternateDimensions;
import qouteall.imm_ptl.peripheral.alternate_dimension.ChaosBiomeSource;
import qouteall.imm_ptl.peripheral.alternate_dimension.ErrorTerrainGenerator;
import qouteall.imm_ptl.peripheral.alternate_dimension.FormulaGenerator;
import qouteall.imm_ptl.peripheral.alternate_dimension.NormalSkylandGenerator;
import qouteall.imm_ptl.peripheral.dim_stack.DimStackGameRule;
import qouteall.imm_ptl.peripheral.dim_stack.DimStackManagement;
import qouteall.imm_ptl.peripheral.guide.IPOuterClientMisc;
import qouteall.imm_ptl.peripheral.mixin.common.end_portal.IEEndDragonFight;
import qouteall.imm_ptl.peripheral.portal_generation.IntrinsicPortalGeneration;
import qouteall.imm_ptl.peripheral.wand.ClientPortalWandPortalDrag;
import qouteall.imm_ptl.peripheral.wand.PortalDraggingAnimation;
import qouteall.imm_ptl.peripheral.wand.PortalWandInteraction;
import qouteall.imm_ptl.peripheral.wand.PortalWandItem;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.LifecycleHack;
import qouteall.q_misc_util.MiscHelper;

import java.util.List;

public class PeripheralModMain {
    
    public static Block portalHelperBlock;
    public static BlockItem portalHelperBlockItem;
    
    @Environment(EnvType.CLIENT)
    public static void initClient() {
        IPOuterClientMisc.initClient();
        
        PortalWandItem.initClient();
    
        ClientPortalWandPortalDrag.init();
    }
    
    public static void init() {
        FormulaGenerator.init();
        
        IntrinsicPortalGeneration.init();
        
        DimStackGameRule.init();
        DimStackManagement.init();
        
        AlternateDimensions.init();
        
        LifecycleHack.markNamespaceStable("immersive_portals");
        LifecycleHack.markNamespaceStable("imm_ptl");
        
        Registry.register(
            BuiltInRegistries.CHUNK_GENERATOR,
            new ResourceLocation("immersive_portals:error_terrain_generator"),
            ErrorTerrainGenerator.codec
        );
        Registry.register(
            BuiltInRegistries.CHUNK_GENERATOR,
            new ResourceLocation("immersive_portals:normal_skyland_generator"),
            NormalSkylandGenerator.codec
        );
        
        Registry.register(
            BuiltInRegistries.BIOME_SOURCE,
            new ResourceLocation("immersive_portals:chaos_biome_source"),
            ChaosBiomeSource.CODEC
        );
        
        EndPortalEntity.updateDragonFightStatusFunc = () -> {
            ServerLevel world = MiscHelper.getServer().getLevel(Level.END);
            if (world == null) {
                return;
            }
            EndDragonFight dragonFight = world.dragonFight();
            if (dragonFight == null) {
                return;
            }
            if (((IEEndDragonFight) dragonFight).ip_getNeedsStateScanning()) {
                ((IEEndDragonFight) dragonFight).ip_scanState();
            }
        };
        
        PortalWandItem.init();
        
        CommandStickItem.init();
    
        PortalWandInteraction.init();
    
//        PortalDraggingAnimation.init();
    }
    
    public static void registerCommandStickTypes() {
        registerPortalSubCommandStick("delete_portal");
        registerPortalSubCommandStick("remove_connected_portals");
        registerPortalSubCommandStick("eradicate_portal_cluster");
        registerPortalSubCommandStick("complete_bi_way_bi_faced_portal");
        registerPortalSubCommandStick("complete_bi_way_portal");
        registerPortalSubCommandStick("move_portal_front", "move_portal 0.5");
        registerPortalSubCommandStick("move_portal_back", "move_portal -0.5");
        registerPortalSubCommandStick(
            "move_portal_destination_front", "move_portal_destination 0.5"
        );
        registerPortalSubCommandStick(
            "move_portal_destination_back", "move_portal_destination -0.5"
        );
        registerPortalSubCommandStick(
            "rotate_x", "rotate_portal_rotation_along x 15"
        );
        registerPortalSubCommandStick(
            "rotate_y", "rotate_portal_rotation_along y 15"
        );
        registerPortalSubCommandStick(
            "rotate_z", "rotate_portal_rotation_along z 15"
        );
        registerPortalSubCommandStick(
            "make_unbreakable", "nbt {unbreakable:true}"
        );
        registerPortalSubCommandStick(
            "make_fuse_view", "nbt {fuseView:true}"
        );
        registerPortalSubCommandStick(
            "enable_pos_adjust", "nbt {adjustPositionAfterTeleport:true}"
        );
        registerPortalSubCommandStick(
            "disable_rendering_yourself", "nbt {doRenderPlayer:false}"
        );
        registerPortalSubCommandStick(
            "enable_isometric", "debug isometric_enable 50"
        );
        registerPortalSubCommandStick(
            "disable_isometric", "debug isometric_disable"
        );
        registerPortalSubCommandStick(
            "create_5_connected_rooms", "create_connected_rooms roomSize 6 4 6 roomNumber 5"
        );
        registerPortalSubCommandStick(
            "accelerate50", "debug accelerate 50"
        );
        registerPortalSubCommandStick(
            "accelerate200", "debug accelerate 200"
        );
        registerPortalSubCommandStick(
            "reverse_accelerate50", "debug accelerate -50"
        );
        registerPortalSubCommandStick(
            "enable_gravity_change", "nbt {teleportChangesGravity:true}"
        );
        registerPortalSubCommandStick(
            "make_invisible", "nbt {isVisible:false}"
        );
        registerPortalSubCommandStick(
            "make_visible", "nbt {isVisible:true}"
        );
        registerPortalSubCommandStick(
            "disable_default_animation", "nbt {defaultAnimation:{durationTicks:0}}"
        );
        
        registerPortalSubCommandStick(
            "pause_animation", "animation pause"
        );
        registerPortalSubCommandStick(
            "resume_animation", "animation resume"
        );
        
        registerPortalSubCommandStick(
            "rotate_around_y", "animation rotate_infinitely @s 0 1 0 1.0"
        );
        registerPortalSubCommandStick(
            "rotate_randomly", "animation rotate_infinitely_random"
        );
        CommandStickItem.registerType(
            "imm_ptl:rotate_around_view",
            new CommandStickItem.Data(
                "execute positioned 0.0 0.0 0.0 run portal animation rotate_infinitely @p ^0.0 ^0.0 ^1.0 1.7",
                "imm_ptl.command.rotate_around_view",
                Lists.newArrayList("imm_ptl.command_dest.rotate_around_view")
            )
        );
        registerPortalSubCommandStick(
            "expand_from_center", "animation expand_from_center 20"
        );
        registerPortalSubCommandStick(
            "clear_animation", "animation clear"
        );
        CommandStickItem.registerType("imm_ptl:reset_scale", new CommandStickItem.Data(
            "/scale set pehkui:base 1",
            "imm_ptl.command.reset_scale",
            Lists.newArrayList("imm_ptl.command_desc.reset_scale")
        ));
        CommandStickItem.registerType("imm_ptl:long_reach", new CommandStickItem.Data(
            "/scale set pehkui:reach 5",
            "imm_ptl.command.long_reach",
            Lists.newArrayList("imm_ptl.command_desc.long_reach")
        ));
        CommandStickItem.registerType("imm_ptl:night_vision", new CommandStickItem.Data(
            "/effect give @s minecraft:night_vision 9999 1 true",
            "imm_ptl.command.night_vision",
            List.of()
        ));
        
        registerPortalSubCommandStick(
            "goback"
        );
        registerPortalSubCommandStick(
            "show_wiki", "wiki"
        );
    }
    
    private static void registerPortalSubCommandStick(String name) {
        registerPortalSubCommandStick(name, name);
    }
    
    private static void registerPortalSubCommandStick(String name, String subCommand) {
        CommandStickItem.registerType("imm_ptl:" + name, new CommandStickItem.Data(
            "/portal " + subCommand,
            "imm_ptl.command." + name,
            Lists.newArrayList("imm_ptl.command_desc." + name)
        ));
    }

//    public static class IndirectMerger {
//        private static final DoubleList EMPTY = DoubleLists.unmodifiable(DoubleArrayList.wrap(new double[]{0.0}));
//        private final double[] result;
//        private final int[] firstIndices;
//        private final int[] secondIndices;
//        private final int resultLength;
//
//        public IndirectMerger(DoubleList l1, DoubleList l2, boolean override1, boolean override2) {
//            double limit = Double.NaN;
//            int size1 = l1.size();
//            int size2 = l2.size();
//            int sumSize = size1 + size2;
//            this.result = new double[sumSize];
//            this.firstIndices = new int[sumSize];
//            this.secondIndices = new int[sumSize];
//            boolean skipEndpointsOf2 = !override1;
//            boolean skipEndpointsOf1 = !override2;
//            int resultIndex = 0;
//            int index1 = 0;
//            int index2 = 0;
//            while (true) {
//                boolean reachLimit1 = index1 >= size1;
//                boolean reachLimit2 = index2 >= size2;
//                if (reachLimit1 && reachLimit2) break;
//                boolean shouldMove1 = !reachLimit1 && (reachLimit2 || l1.getDouble(index1) < l2.getDouble(index2) + 1.0E-7);
//                if (shouldMove1) {
//                    ++index1;
//                    if (skipEndpointsOf2 && (index2 == 0 || reachLimit2)) {
//                        continue;
//                    }
//                } else {
//                    ++index2;
//                    if (skipEndpointsOf1 && (index1 == 0 || reachLimit1)) continue;
//                }
//                int lastIndex1 = index1 - 1;
//                int lastIndex2 = index2 - 1;
//                double number = shouldMove1 ? l1.getDouble(lastIndex1) : l2.getDouble(lastIndex2);
//                if (!(limit >= number - 1.0E-7)) {
//                    this.firstIndices[resultIndex] = lastIndex1;
//                    this.secondIndices[resultIndex] = lastIndex2;
//                    this.result[resultIndex] = number;
//                    ++resultIndex;
//                    limit = number;
//                    continue;
//                }
//                this.firstIndices[resultIndex - 1] = lastIndex1;
//                this.secondIndices[resultIndex - 1] = lastIndex2;
//            }
//            this.resultLength = Math.max(1, resultIndex);
//        }
//    }


}
