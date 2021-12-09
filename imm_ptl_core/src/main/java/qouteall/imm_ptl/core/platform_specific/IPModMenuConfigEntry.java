package qouteall.imm_ptl.core.platform_specific;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class IPModMenuConfigEntry implements ModMenuApi {
    
    public IPModMenuConfigEntry() {}
    
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return IPConfigGUI::createClothConfigScreen;
    }
    
}
