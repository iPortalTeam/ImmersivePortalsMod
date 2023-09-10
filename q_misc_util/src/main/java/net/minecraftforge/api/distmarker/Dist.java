package net.minecraftforge.api.distmarker;

/**
 * A fake dependency for making the developing the Forge version of Immersive Portals mod easier.
 * It's kept the same as Forge.
 */
public enum Dist {
    CLIENT,
    DEDICATED_SERVER;
    
    public boolean isDedicatedServer()
    {
        return !isClient();
    }
    
    public boolean isClient()
    {
        return this == CLIENT;
    }
}
