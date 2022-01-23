package qouteall.imm_ptl.peripheral;

import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.imm_ptl.peripheral.alternate_dimension.AlternateDimensions;
import qouteall.imm_ptl.peripheral.alternate_dimension.FormulaGenerator;
import qouteall.imm_ptl.peripheral.altius_world.AltiusGameRule;
import qouteall.imm_ptl.peripheral.altius_world.AltiusManagement;
import qouteall.imm_ptl.peripheral.guide.IPGuide;
import qouteall.imm_ptl.peripheral.portal_generation.IntrinsicPortalGeneration;

public class PeripheralModMain {
    
    public static Block portalHelperBlock;
    public static BlockItem portalHelperBlockItem;
    
    @Environment(EnvType.CLIENT)
    public static void initClient() {
        IPGuide.initClient();
    }
    
    public static void init() {
        FormulaGenerator.init();
        
        IntrinsicPortalGeneration.init();
        
        AltiusGameRule.init();
        AltiusManagement.init();
        
        AlternateDimensions.init();
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
            "make_unbreakable", "set_portal_nbt {unbreakable:true}"
        );
        registerPortalSubCommandStick(
            "make_fuse_view", "set_portal_nbt {fuseView:true}"
        );
        registerPortalSubCommandStick(
            "disable_rendering_yourself", "set_portal_nbt {doRenderPlayer:false}"
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
            "enable_gravity_change", "set_portal_nbt {teleportChangesGravity:true}"
        );
        if (O_O.getIsPehkuiPresent()) {
            //PehkuiInterface.isPehkuiPresent may not be initialized in time
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
        }
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
