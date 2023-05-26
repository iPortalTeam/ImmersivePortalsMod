package qouteall.imm_ptl.core.platform_specific;

import com.fusionflux.gravity_api.util.GravityChannel;
import com.fusionflux.gravity_api.util.packet.DefaultGravityPacket;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import qouteall.imm_ptl.core.IPModMain;
import qouteall.imm_ptl.core.compat.GravityChangerInterface;
import qouteall.imm_ptl.core.compat.IPVanishCompat;
import qouteall.q_misc_util.Helper;

public class IPModEntry implements ModInitializer {
    
    @Override
    public void onInitialize() {
        IPModMain.init();
        RequiemCompat.init();
        IPVanishCompat.init();
        
        IPRegistry.registerEntitiesFabric();
        
        IPRegistry.registerMyDimensionsFabric();
        
        IPRegistry.registerBlocksFabric();
        
        IPRegistry.registerChunkGenerators();
        
        if (FabricLoader.getInstance().isModLoaded("dimthread")) {
            O_O.isDimensionalThreadingPresent = true;
            Helper.log("Dimensional Threading is present");
        }
        else {
            Helper.log("Dimensional Threading is not present");
        }
        
        if (O_O.getIsPehkuiPresent()) {
            PehkuiInterfaceInitializer.init();
            Helper.log("Pehkui is present");
        }
        else {
            Helper.log("Pehkui is not present");
        }
        
        if (FabricLoader.getInstance().isModLoaded("gravity_api")) {
            GravityChangerInterface.invoker = new GravityChangerInterface.OnGravityChangerPresent();
            Helper.log("Gravity API is present");
        }
        else {
            Helper.log("Gravity API is not present");
        }
        
    }
    
}
