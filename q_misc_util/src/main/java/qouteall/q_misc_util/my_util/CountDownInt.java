package qouteall.q_misc_util.my_util;

public class CountDownInt {
    private int value;
    
    public CountDownInt(int value) {
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
