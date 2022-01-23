package qouteall.imm_ptl.peripheral.ducks;

import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.DataPackConfig;

public interface IECreateWorldScreen {
    PackRepository portal_getResourcePackManager();
    
    DataPackConfig portal_getDataPackSettings();
}
