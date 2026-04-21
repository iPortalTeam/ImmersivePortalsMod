package qouteall.imm_ptl.core.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.ladysnake.cca.api.v3.block.BlockEntitySyncCallback;
import qouteall.q_misc_util.Helper;

public class IPCardinalCompBlockCompat {
    public static boolean isCardinalCompBlockPresent = false;

    public static void init() {
        if (FabricLoader.getInstance().isModLoaded("cardinal-components-block")) {
            Helper.log("Cardinal Components Block is present");
            isCardinalCompBlockPresent = true;
        }
    }

    public static void syncBlockEntities(ServerPlayer player, LevelChunk chunk) {
        if (isCardinalCompBlockPresent) {
            for (BlockEntity be : chunk.getBlockEntities().values()) {
                BlockEntitySyncCallback.EVENT.invoker().onBlockEntitySync(player, be);
            }
        }
    }
}
