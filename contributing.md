To develop this mod for both Fabric and Forge platform, my approach is to mainly develop the Fabric version and
 remap the platform-independent code from yarn into mcp and put into the Forge version.
The platform-specific code is in the weird-named `com.qouteall.hiding_in_the_bushes` package. All other code are cross-platform.
All Fabric specific code should go into `com.qouteall.hiding_in_the_bushes` package.
