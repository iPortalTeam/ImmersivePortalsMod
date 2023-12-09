package qouteall.q_misc_util.my_util;

import org.apache.commons.lang3.Validate;

public class CountDownInt {
    private int value;
    
    public CountDownInt(int value) {
        Validate.isTrue(value >= 0);
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }
    
    public boolean tryDecrement() {
        if (value > 0) {
            value--;
            return true;
        }
        else {
            return false;
        }
    }
}
