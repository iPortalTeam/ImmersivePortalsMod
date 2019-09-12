package com.qouteall.immersive_portals.mixin_client;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.minecraft.util.profiler.ProfileResultImpl;
import net.minecraft.util.profiler.ProfilerTiming;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.*;

@Mixin(ProfileResultImpl.class)
public class MixinProfileResultImpl {
    @Shadow
    @Final
    private Map<String, Long> timings;
    @Shadow
    @Final
    private Map<String, Long> field_19382;
    
    /**
     * @author qouteall
     * @reason this vanilla functionality seems not working. mojang did not fix it, then I fix it
     */
    @Overwrite
    public List<ProfilerTiming> getTimings(String key) {
        long long_1 = this.timings.containsKey("root") ? (Long) this.timings.get("root") : 0L;
        long long_2 = (Long) this.timings.getOrDefault(key, -1L);
        long long_3 = (Long) this.field_19382.getOrDefault(key, 0L);
        List<ProfilerTiming> list_1 = Lists.newArrayList();
        if (!key.isEmpty()) {
            key = key + '\u001e';
        }
        
        //this is what I added
        key = key.replace('.', '\u001e');
        
        long long_4 = 0L;
        Iterator var12 = this.timings.keySet().iterator();
        
        while (var12.hasNext()) {
            String string_3 = (String) var12.next();
            if (string_3.length() > key.length() && string_3.startsWith(key) && string_3.indexOf(
                30,
                key.length() + 1
            ) < 0) {
                long_4 += (Long) this.timings.get(string_3);
            }
        }
        
        float float_1 = (float) long_4;
        if (long_4 < long_2) {
            long_4 = long_2;
        }
        
        if (long_1 < long_4) {
            long_1 = long_4;
        }
        
        Set<String> profileKeySet = Sets.newHashSet(this.timings.keySet());
        profileKeySet.addAll(this.field_19382.keySet());
        Iterator keySetIter = profileKeySet.iterator();
        
        String currProfile;
        while (keySetIter.hasNext()) {
            currProfile = (String) keySetIter.next();
            if (currProfile.length() > key.length() && currProfile.startsWith(key) && currProfile.indexOf(
                30,
                key.length() + 1
            ) < 0) {
                long long_5 = (Long) this.timings.getOrDefault(currProfile, 0L);
                double double_1 = (double) long_5 * 100.0D / (double) long_4;
                double double_2 = (double) long_5 * 100.0D / (double) long_1;
                String string_5 = currProfile.substring(key.length());
                long long_6 = (Long) this.field_19382.getOrDefault(currProfile, 0L);
                list_1.add(new ProfilerTiming(string_5, double_1, double_2, long_6));
            }
        }
        
        keySetIter = this.timings.keySet().iterator();
        
        while (keySetIter.hasNext()) {
            currProfile = (String) keySetIter.next();
            this.timings.put(currProfile, (Long) this.timings.get(currProfile) * 999L / 1000L);
        }
        
        if ((float) long_4 > float_1) {
            list_1.add(new ProfilerTiming(
                "unspecified",
                (double) ((float) long_4 - float_1) * 100.0D / (double) long_4,
                (double) ((float) long_4 - float_1) * 100.0D / (double) long_1,
                long_3
            ));
        }
        
        Collections.sort(list_1);
        list_1.add(
            0,
            new ProfilerTiming(key, 100.0D, (double) long_4 * 100.0D / (double) long_1, long_3)
        );
        return list_1;
    }
}
