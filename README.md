# Immersive Portals Mod

It's a minecraft mod that provides see-through portals and seamless teleportation.

https://www.curseforge.com/minecraft/mc-mods/immersive-portals-mod

This is one of the most complex minecraft mods.
* It eliminates the limitation that only one dimension can be loaded and rendered on client
* It eliminates the limitation that only the chunks nearby player can be synchronized to client
* It eliminates the limitation that client can only load and render chunks nearby the player
* The portal rendering is recursive which needs proper management of minecraft rendering context of multiple dimensions
* The portal can have rotation, scale and mirror transformations
* The teleportation is seamless, creating the illusion that the space is connected
* Cross portal collision
* Cross portal entity rendering
* Place and break blocks through portal
* Portal frame matching (supports any shaped portal frame and adaptive matching)

Portals let the game loads more chunk and render more chunks which creates additional performance cost.
This mod has some lag spike optimizations to reduce the client lag spike caused by abruptly loading
 and unloading many chunks (defer remote sections rebuild, merge glGenBuffers, delay chunk unload ...).
 It also provides performance configurations to adjust the performance.

Many optimizations have been made to portal rendering (inner and outer frustum culling) but several additional
 optimizations are being planned (merged portal rendering, occlusion query prediction)

## How to run this code
https://fabricmc.net/wiki/tutorial:setup

## Wiki
https://github.com/qouteall/ImmersivePortalsMod/wiki

## Discord Server
https://discord.gg/BZxgURK