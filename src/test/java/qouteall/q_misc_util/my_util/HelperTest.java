package qouteall.q_misc_util.my_util;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.Test;
import qouteall.q_misc_util.Helper;

public class HelperTest {
    @Test
    public void testRemoveIfEarlyExit() {
        ObjectList<Integer> list = ObjectArrayList.of(1, 2, 3, 9, 1, 2, 3);
        Helper.removeIfWithEarlyExit(
            list, (value, shouldEarlyExit) -> {
                if (value > 3) {
                    shouldEarlyExit.setValue(true);
                }
                return value <= 3;
            }
        );
        Validate.isTrue(list.equals(ObjectArrayList.of(9, 1, 2, 3)));
    }
}
