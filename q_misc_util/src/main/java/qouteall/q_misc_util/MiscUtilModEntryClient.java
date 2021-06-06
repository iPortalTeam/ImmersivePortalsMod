package qouteall.q_misc_util;

import net.fabricmc.api.ClientModInitializer;

public class MiscUtilModEntryClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MiscNetworking.initClient();
    }
}
