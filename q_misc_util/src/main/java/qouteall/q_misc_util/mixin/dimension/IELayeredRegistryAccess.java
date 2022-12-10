package qouteall.q_misc_util.mixin.dimension;

import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.RegistryAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(LayeredRegistryAccess.class)
public interface IELayeredRegistryAccess {
    @Invoker("<init>")
    public static <T> LayeredRegistryAccess<T> ip_init(List<T> list, List<RegistryAccess.Frozen> list2){
        throw new RuntimeException();
    }
}
