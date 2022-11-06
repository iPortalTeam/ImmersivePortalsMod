package qouteall.imm_ptl.peripheral;

import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import qouteall.imm_ptl.peripheral.alternate_dimension.AlternateDimensions;
import qouteall.imm_ptl.peripheral.alternate_dimension.ChaosBiomeSource;
import qouteall.imm_ptl.peripheral.alternate_dimension.ErrorTerrainGenerator;
import qouteall.imm_ptl.peripheral.alternate_dimension.FormulaGenerator;
import qouteall.imm_ptl.peripheral.alternate_dimension.NormalSkylandGenerator;
import qouteall.imm_ptl.peripheral.dim_stack.DimStackGameRule;
import qouteall.imm_ptl.peripheral.dim_stack.DimStackManagement;
import qouteall.imm_ptl.peripheral.guide.IPOuterClientMisc;
import qouteall.imm_ptl.peripheral.portal_generation.IntrinsicPortalGeneration;

import java.util.List;

public class PeripheralModMain {
    
    public static Block portalHelperBlock;
    public static BlockItem portalHelperBlockItem;
    
    @Environment(EnvType.CLIENT)
    public static void initClient() {
        IPOuterClientMisc.initClient();
    }
    
    public static void init() {
        FormulaGenerator.init();
        
        IntrinsicPortalGeneration.init();
        
        DimStackGameRule.init();
        DimStackManagement.init();
        
        AlternateDimensions.init();
        
        Registry.register(
            Registry.CHUNK_GENERATOR,
            new ResourceLocation("immersive_portals:error_terrain_generator"),
            ErrorTerrainGenerator.codec
        );
        Registry.register(
            Registry.CHUNK_GENERATOR,
            new ResourceLocation("immersive_portals:normal_skyland_generator"),
            NormalSkylandGenerator.codec
        );
    
        Registry.register(
            Registry.BIOME_SOURCE,
            new ResourceLocation("immersive_portals:chaos_biome_source"),
            ChaosBiomeSource.CODEC
        );
    }
    
    public static void registerCommandStickTypes() {
        registerPortalSubCommandStick("delete_portal");
        registerPortalSubCommandStick("remove_connected_portals");
        registerPortalSubCommandStick("eradicate_portal_cluster");
        registerPortalSubCommandStick("complete_bi_way_bi_faced_portal");
        registerPortalSubCommandStick("complete_bi_way_portal");
        registerPortalSubCommandStick(
            "bind_cluster", "set_portal_nbt {bindCluster:true}"
        );
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
            "rotate_around_me", "animation rotate_infinitely @s 0 1 0 1.5"
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
}
