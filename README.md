# Immersive Portals Mod

It's a minecraft mod that provides see-through portals and seamless teleportation. It can create "Non-Euclidean" (Uneuclidean) space effect.

![immptl.png](https://i.loli.net/2021/09/30/chHMG45dsnZNqep.png)

CurseForge: https://www.curseforge.com/minecraft/mc-mods/immersive-portals-mod

Website: https://qouteall.fun/immptl

This is one of the most complex Minecraft mods. [Implementation Details](https://qouteall.fun/immptl/wiki/Implementation-Details)
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

Many optimizations have been made to portal rendering which includes:
* Do aggressive frustum culling during portal rendering
* Do aggressive frustum culling for the sections that are hidden by the portal during outer world rendering
* Reduce occlusion query stalls by utilizing temporal coherence
* Merge the rendering of the portals that have the same spacial transformation (for example the scale box end portal)


## How to run this code
https://fabricmc.net/wiki/tutorial:setup

## Wiki
https://qouteall.fun/immptl/wiki

## Discord Server
https://discord.gg/BZxgURK