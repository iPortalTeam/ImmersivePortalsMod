package qouteall.q_misc_util.my_util;

/**
 * Similar to a pointer, we can get or set the value of a variable.
 */
public interface Access<T> {
    T get();
    
    void set(T t);
}
