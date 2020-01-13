## What is this
It's a minecraft mod.

https://www.curseforge.com/minecraft/mc-mods/immersive-portals-mod

## How to run this code
1. ```gradlew idea``` or ```gradlew eclipse```
2. Put OptiFine jar into run/mods folder
This project has dependency to OptiFabric which needs OptiFine jar to run

ofstuff1.14yarn.jar is a part of OptiFine and is used as a local dependency.

## Briefly explain how this mod works

This mod hacks a large range of vanilla mechanics.

### Portal entity
A portal entity is one-way and one-faced.
A normal nether portal contains 2 portals in overworld and 2 portals in nether.

When lighting a nether portal, it will firstly try to link to the obsidian
 frame with same shape and direction in 150 range in the other dimension.
The frame searching will be done on server thread but the task will be splited so
 that the server will not froze.

### Rendering
Portal rendering takes place before translucent block rendering.
It uses stencil buffer and does not use additional frame buffer when rendering without shaders.

It firstly draw the portal view area.
When the camera is very close to the portal,
the pixels that are too close to the camera are culled during rasterization.
So it will additionally render a small pyramid-shaped hood around camera.

Drawing view area will increase stencil value by one.
Then it will move the player to another dimension, switch objects for rendering 
and render the world again in stencil limitation.
It will then render nested portals recursively.

ChunkRenderDispatcher stores an fixed size array of ChunkRenderer s.
I changed it into a map.

While rendering portal content, it will cull pixels behind portal and it
will do more strict frustum culling to improve performance.

![](https://i.ibb.co/tHJv6ZH/2019-09-05-17-10-47.png)
![](https://i.ibb.co/y8JVVxH/2019-09-05-17-10-53.png)

### Server side chunk loading

In server side, it will send redirected packet to players to synchronize world information.
If the packet is not redirected, a chunk data packet of nether may be recognized as overworld chunk data in client.

I made my own chunk loading determination logic.
I tried to use my own chunk ticket to load the chunk but it didn't work as intended.
Currently it invokes vanilla's force loading mechanic to load chunks.
So /forceload command with this mod will not work.

This mod will delay the unloading of chunks to avoid frequently load/unload chunks.
 So it will consume more memory.

### Client side world data management
Minecraft vanilla assumes that only one dimension would exist in client at the same time.
But this mod breaks that assumption.
This mod will create faked client world when it's firstly used.
When ticking remote world or processing remote world packet, it will switch the world field of
MinecraftClient and then switch back.

Vanilla assumes that only the chunks near player will be loaded so ClientChunkManager
use a fixed size 2d array to store chunks.
I change it into a map so the chunk storage is not limited to the area near player.
Similar thing also applies to ChunkRenderDispatcher.

### Seamless teleportation
Teleportation on client side happens before rendering (not during ticking).
Teleportation happens when the camera cross the portal (not after the player entity crossing the portal).

Client will teleport first and then the server receives the teleport request and do teleportation on server.

### Collision
When an entity is halfway in portal then its collision will be specially treated.
It will cut the entity's collision into two pieces, one outside portal and one inside portal.
It firstly do collision test for outer part and then
 switch the entity to the portal destination position and do collision test for inner part.
 
### Global Portals
World wrapping portals and vertical dimension connecting portals are very big.
They are not entities in world like small portals.
They are stored in per dimension global portal storage.