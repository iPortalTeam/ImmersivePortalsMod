package qouteall.q_misc_util.platform_specific;

/**
 * An alternative would be to directly implement isClient() and isServer into {@link PlatformHelper}.
 * @see net.fabricmc.api.EnvType
 */
public enum EnvType {
    CLIENT,
    SERVER,
    UNIMPLEMENTED
}
