## What is this
It's a minecraft mod.

https://www.curseforge.com/minecraft/mc-mods/immersive-portals-mod

## How to run this code
1. ```gradlew idea``` or ```gradlew eclipse```

## Briefly explain how this mod works

This mod hacks a large range of vanilla mechanics.

### Portal Rendering

There are two ways of rendering portal within the architecture of rasterization:
1. Use stencil buffer to limit rendering area and render portals from outer to inner
2. Render portal content to another frame buffer first and then draw to the main frame buffer. Render inner portal first.

This mod adopts the first method.
(It also has the "compatibility" rendering mode which use the second method but does not support portal-in-portal rendering)

Portal rendering takes place after all solid triangle rendering and before translucent triangle rendering.
It will firstly render the portals nearby camera by the order of distance to camera.
When rendering a portal, it will use occlusion query to determine whether a portal is visible.
If the portal is visible, clear the depth of portal view area, 
and then switch the rendering context and render the world again.
And it will render inner portals recursively.

The outmost part of the framebuffer has stencil value 0.
Rendering the portal area will increase stencil value by 1.
So the stencil value corresponds to portal layer.

When rendering portal view area, if camera is very close to the portal,
the pixels that are too close to the camera are culled during rasterization.
To avoid this I additionally render a small pyramid-shaped hood around camera in this case.
(I used the method of rendering two layers in earlier versions.
That method is not perfect with world wrapping portal.
Sometimes contents between two layers will not be covered by portal area)

#### Advanced Frustum Culling
When rendering portals, it will do advanced frustum culling to improve performance.

For example, when rendering this scene
![](https://i.ibb.co/nrpxQZk/2020-03-06-22-00-40.png)

The sections behind the portal will be culled
![](https://i.ibb.co/N25Y3hB/2020-03-06-21-59-37.png)

The sections out of portal view will be culled
![](https://i.ibb.co/sFGMwCd/2020-03-06-21-59-43.png)

Without advanced frustum culling:
![](https://i.ibb.co/k6nvDbf/2020-03-06-22-00-05.png)
![](https://i.ibb.co/wY5sYXM/2020-03-06-21-59-56.png)


#### Front Culling

When rendering portal content, everything in front of the portal 
 plane will be culled pixel-wise.

These pixels will block the portal view if not being culled
![](https://i.ibb.co/4Yf48sq/2020-03-06-22-00-16.png)
![](https://i.ibb.co/nrpxQZk/2020-03-06-22-00-40.png)

There is another culling method using oblique projection.
http://www.terathon.com/lengyel/Lengyel-Oblique.pdf
But oblique projection won't work when the angle between culling plane normal with
 view vector is not bigger than 90 degree.

(Rendering portals and mirrors with ray tracing is much simpler than in rasterization.)

### Portal entity
A portal entity is one-way and one-faced.
A normal nether portal contains 2 portals in overworld and 2 portals in nether.

When lighting a nether portal, it will firstly try to link to the obsidian
 frame with same shape and direction in 150 range in the other dimension.
The frame searching will be done on server thread but the task will be splited so
 that the server will not froze.
 
### Breaking hacks
Vanilla assumes that only the chunks near player will be loaded so ClientChunkManager
use a fixed size 2d array to store chunks.
I change it into a map so the chunk storage is not limited to the area near player.
Similar thing also applies to BuiltChunkStorage.

Vanilla only allows one client world to be present at a time but this mod breaks this assumption.
This mod will create faked client world when it's firstly used.
When ticking remote world or processing remote world packet, it will switch the world field of
MinecraftClient and then switch back.

### Chunk loading

In server side, it will send redirected packet to players to synchronize world information.
If the packet is not redirected, a chunk data packet of nether may be recognized as overworld chunk data in client.

I made my own chunk loading determination logic.
I tried to use my own chunk ticket to load the chunk but it didn't work as intended.
Currently it invokes vanilla's force loading mechanic to load chunks.
So /forceload command with this mod will not work.

This mod will delay the unloading of chunks to avoid frequently load/unload chunks.
 So it will consume more memory.

### Seamless teleportation
Teleportation on client side happens before rendering (not during ticking).
Teleportation happens when the camera cross the portal (not after the player entity crossing the portal).

Client will teleport first and then the server receives the teleport request and do teleportation on server.
But when the player is not teleporting frequently, client will accept sync message from server.

### Collision
When an entity is halfway in portal then its collision will be specially treated.
It will cut the entity's collision into two pieces, one outside portal and one inside portal.
It firstly do collision test for outer part and then
 switch the entity to the portal destination position and do collision test for inner part.

The plane for cutting the collision box is not the portal plane.
It is the portal plane moved by the reverse of moving attempt vector.
 
### Global Portals
World wrapping portals and vertical dimension connecting portals are very big.
They are not entities in world like small portals.
They are stored in per dimension global portal storage.

