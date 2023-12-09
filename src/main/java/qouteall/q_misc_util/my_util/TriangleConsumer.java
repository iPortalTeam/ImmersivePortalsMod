package qouteall.q_misc_util.my_util;

@FunctionalInterface
public interface TriangleConsumer {
    void accept(
        double p0x, double p0y, double p0z,
        double p1x, double p1y, double p1z,
        double p2x, double p2y, double p2z
    );
}
