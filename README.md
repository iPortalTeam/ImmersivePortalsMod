# Immersive Portals Mod

It's a minecraft mod that provides see-through portals and seamless teleportation.

https://www.curseforge.com/minecraft/mc-mods/immersive-portals-mod

(The experimental website: https://qouteall.fun/immptl)

This is one of the most complex minecraft mods.
* It eliminates the limitation that only one dimension can be loaded and rendered on client
* It eliminates the limitation that only the chunks nearby player can be synchronized to client
* It eliminates the limitation that client can only load and render chunks nearby the player
* The portal rendering is recursive which needs proper management of minecraft rendering context of multiple dimensions
* The portal can have rotation, scale and mirror transformations
* The teleportation is seamless, creating the feel that the space is connected
* Cross portal collision
* Cross portal entity rendering
* Place and break blocks through portal
* Portal frame matching (supports any shaped portal frame and adaptive matching)

Portals let the game loads more chunk and render more chunks which creates additional performance cost.

In vanilla when the player moves only the edge chunks are loaded/unloaded.
 But with this mod approaching/getting away from portals cause the game to abruptly load and
 unload many chunks which cause lag spikes. These optimizations are used to mitigate the lag spikes:
* Reduce the sections being rebuild during portal rendering
* Change portal render distance gradually on the server side
* Defer chunk unloading
* Defer the client light updates caused by chunk unloading
* Merge glGenBuffer calls

It also provides performance configurations to adjust the performance.

Many optimizations have been made to portal rendering which includes:
* Do aggressive frustum culling during portal rendering
* Do aggressive frustum culling for the sections that are hidden by the portal during outer world rendering
* Reduce occlusion query stalls by utilizing temporal coherence

## How to run this code
https://fabricmc.net/wiki/tutorial:setup

## Wiki
https://qouteall.fun/immptl/wiki

## Discord Server
https://discord.gg/BZxgURK