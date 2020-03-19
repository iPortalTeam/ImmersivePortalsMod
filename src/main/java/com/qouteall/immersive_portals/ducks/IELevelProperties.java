package com.qouteall.immersive_portals.ducks;

import com.qouteall.immersive_portals.altius_world.AltiusInfo;
import com.sun.istack.internal.Nullable;

public interface IELevelProperties {
    @Nullable
    public AltiusInfo getAltiusInfo();
    
    public void setAltiusInfo(AltiusInfo cond);
}
