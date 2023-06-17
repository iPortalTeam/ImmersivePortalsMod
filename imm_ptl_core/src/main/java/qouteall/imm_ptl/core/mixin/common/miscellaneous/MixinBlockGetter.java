package qouteall.imm_ptl.core.mixin.common.miscellaneous;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import qouteall.imm_ptl.core.IPMcHelper;
import qouteall.q_misc_util.Helper;

import java.util.function.BiFunction;
import java.util.function.Function;

@Mixin(BlockGetter.class)
public interface MixinBlockGetter {
	
	// avoid lagging due to long block traversal
	@ModifyVariable(
		method = "traverseBlocks",
		at = @At("HEAD"),
		argsOnly = true,
		index = 1
	)
	private static <T, C> Vec3 onTraverseBlocks(
		Vec3 originalArgument,
		Vec3 from, Vec3 _to, C context,
		BiFunction<C, BlockPos, T> tester, Function<C, T> onFail
	) {
		if (from.distanceToSqr(_to) > (512 * 512)) {
			IPMcHelper.limitedLogger.invoke(() -> {
				Helper.logger.error("Raycast too far", new Throwable());
			});
			return _to.subtract(from).normalize().scale(30).add(from);
		}
		return _to;
	}
	
}
